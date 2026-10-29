#include"types.h"
#include"movement.h"

#include<errno.h>
#include<signal.h>
#include<stdio.h>
#include<stdlib.h>
#include<string.h>
#include<unistd.h>
#include<sys/stat.h>

static volatile sig_atomic_t time_to_go = 0;

static void handle_violation(int sig){
    const char msg[] = "Flight: SAFETY VIOLATION!\n";

    (void)sig;
    write(STDOUT_FILENO, msg, sizeof(msg) - 1);
}

static void handle_term(int sig){
    (void)sig;
    time_to_go = 1;
}

static void install_handlers(void){
    struct sigaction act;

    memset(&act, 0, sizeof(struct sigaction));
    /* US107 AC: while a violation (SIGUSR1) is handled, all other signals are
     * blocked (sigfillset on sa_mask) so the handler is not interrupted. */
    sigfillset(&act.sa_mask);

    act.sa_handler = handle_violation;
    if(sigaction(SIGUSR1, &act, NULL) == -1)
        _exit(FLIGHT_EXIT_WRITE_ERROR);

    act.sa_handler = handle_term;
    if(sigaction(SIGTERM, &act, NULL) == -1)
        _exit(FLIGHT_EXIT_WRITE_ERROR);
}

void flight_process(flight_t *flight, int write_fd, int total_steps,
                    double dt){
    int step;
    position_update_t update;
    /* US105 AC4: memória partilhada + semáforos */
    int         shm_fd;
    SharedData *shm;
    sem_t      *sem_mutex;
    sem_t      *sem_barrier;
    sem_t      *sem_nproc;
    sem_t      *sem_tick;    /* US108: per-step gate (parent -> this flight) */
    sem_t      *sem_done;    /* US108: per-step ack  (this flight -> parent) */
    char        tick_name[SEM_TICK_NAME_LEN];

    install_handlers();

    /* US105 AC3+AC4: abrir shm e semáforos já criados pelo pai */
    shm_fd = shm_open(SHM_NAME, O_RDWR, 0);
    if(shm_fd == -1) _exit(FLIGHT_EXIT_WRITE_ERROR);
    shm = mmap(NULL, sizeof(SharedData), PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
    if(shm == MAP_FAILED) _exit(FLIGHT_EXIT_WRITE_ERROR);

    sem_mutex   = sem_open(SEM_MUTEX,   0);
    sem_barrier = sem_open(SEM_BARRIER, 0);
    sem_nproc   = sem_open(SEM_NPROC,   0);
    /* US108: open the step semaphores created by the parent before fork.
     * this flight only opens its own tick (one tick per flight index). */
    snprintf(tick_name, sizeof(tick_name), "%s%d", SEM_TICK_PREFIX,
             flight->index);
    sem_tick    = sem_open(tick_name,   0);
    sem_done    = sem_open(SEM_DONE,    0);
    if(sem_mutex == SEM_FAILED || sem_barrier == SEM_FAILED || sem_nproc == SEM_FAILED ||
       sem_tick == SEM_FAILED || sem_done == SEM_FAILED)
        _exit(FLIGHT_EXIT_WRITE_ERROR);

    for(step=0; step<total_steps && !time_to_go; step++){

        /* US108 AC1: the flight blocks here until the parent opens this step by
         * posting sem_tick (replaces the Sprint-2 SIGUSR2 tick). On early
         * termination the parent stops opening steps; to avoid a deadlock in
         * the check-then-block window (SIGTERM landing between the test and the
         * wait), the parent also clears shm->simulation_running and posts one
         * drain token per flight. */
        while(sem_wait(sem_tick) == -1 && errno == EINTR && !time_to_go)
            ;   /* retry unless we were told to terminate */

        if(time_to_go || !shm->simulation_running)
            break;

        /* A flight is active only between its entry and its arrival:
           - before entry_step it holds at its area-entry point;
           - once it has arrived at its final waypoint it holds there.
           In both inactive states it does not advance and detection skips it,
           but it still writes one update per step so the parent's per-step
           drain stays in sync. */
        int entered = (step >= flight->entry_step);

        /* movement via physics; wind is 0,0 for Sprint 2. advance_flight
           sets x/y/alt and the derived vx/vy/vz (metres per step), and may
           flip flight->arrived to 1 on this step when the route completes. */
        double prev_x = flight->x;
        double prev_y = flight->y;
        double prev_alt = flight->alt;
        if(entered && !flight->arrived){
            advance_flight(flight, dt, 0.0, 0.0);
        } else {
            flight->vx = 0.0;
            flight->vy = 0.0;
            flight->vz = 0.0;
        }

        /* recompute after advance: a flight that arrives this step is already
           inactive for shm/detection purposes from this step onward. */
        int active = entered && !flight->arrived;

        update.flight_index = flight->index;
        update.flight_id = flight->id;
        update.step = step;
        update.x = flight->x;
        update.y = flight->y;
        update.alt = flight->alt;
        update.vx = flight->vx;
        update.vy = flight->vy;
        update.vz = flight->vz;

        /* US105 AC4: escrita atómica na shm protegida por semáforo */
        while(sem_wait(sem_mutex) == -1 && errno == EINTR)
            ;
        shm->flights[flight->index].active   = active ? 1 : 0;
        shm->flights[flight->index].step     = step;
        shm->flights[flight->index].prev_x   = prev_x;
        shm->flights[flight->index].prev_y   = prev_y;
        shm->flights[flight->index].prev_alt = prev_alt;
        shm->flights[flight->index].x        = flight->x;
        shm->flights[flight->index].y        = flight->y;
        shm->flights[flight->index].alt      = flight->alt;
        shm->flights[flight->index].vx       = flight->vx;
        shm->flights[flight->index].vy       = flight->vy;
        shm->flights[flight->index].vz       = flight->vz;
        sem_post(sem_mutex);

        if(write_full(write_fd, &update, sizeof(position_update_t)) != 0){
            /* US108: still ack this step so the parent's sem_wait(sem_done)
             * barrier does not block forever, then terminate. */
            sem_post(sem_done);
            sem_close(sem_mutex);
            sem_close(sem_barrier);
            sem_close(sem_nproc);
            sem_close(sem_tick);
            sem_close(sem_done);
            munmap(shm, sizeof(SharedData));
            close(shm_fd);
            close(write_fd);
            _exit(FLIGHT_EXIT_WRITE_ERROR);
        }

        /* US108 AC2: signal the parent that this flight finished step `step`.
         * The parent collects N of these before advancing to step+1, so all
         * flights complete a step before any starts the next. */

        sem_post(sem_done);
    }

    /* Marcar voo como terminado na shm */
    while(sem_wait(sem_mutex) == -1 && errno == EINTR)
        ;
    shm->flights[flight->index].active = 0;
    sem_post(sem_mutex);

    /* US105 AC4: barreira de sincronização — todos os voos chegam aqui antes de sair */
    while(sem_wait(sem_nproc) == -1 && errno == EINTR)
        ;
    shm->nproc_at_barrier++;
    if(shm->nproc_at_barrier == shm->total_flights)
        sem_post(sem_barrier);   /* último voo desbloqueia o primeiro */
    sem_post(sem_nproc);

    while(sem_wait(sem_barrier) == -1 && errno == EINTR)
        ;
    sem_post(sem_barrier);       /* cascade: permite que o próximo passe */

    /* Limpeza do processo filho */
    sem_close(sem_mutex);
    sem_close(sem_barrier);
    sem_close(sem_nproc);
    sem_close(sem_tick);
    sem_close(sem_done);
    munmap(shm, sizeof(SharedData));
    close(shm_fd);

    close(write_fd);
    if(time_to_go)
        _exit(FLIGHT_EXIT_TERMINATED);

    _exit(FLIGHT_EXIT_COMPLETED);
}
