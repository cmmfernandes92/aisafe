#include "types.h"
#include "detection_thread.h"
#include "report_thread.h"
#include "environment_thread.h"

#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/* ── US105: thread globals (process-private, not in shm) ── */
static pthread_mutex_t g_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_cond  = PTHREAD_COND_INITIALIZER;
static int g_flights_done  = 0;
static int g_total_flights = 0;

typedef struct {
    SharedData *shm;
    sem_t      *sem_mutex;
} monitor_arg_t;

static void twrite(const char *msg){
    write(STDOUT_FILENO, msg, strlen(msg));
}

/* US105 AC1 — thread dedicada de monitorização: lê shm periodicamente.
 * [SM/Sem] shared reads under the mutex: snapshot simulation_running +
 * total_flights + flights[] numa única secção crítica para uma vista
 * consistente; formata e escreve fora do lock para manter o CS curto. */
static void *monitor_thread(void *arg){
    monitor_arg_t *m   = (monitor_arg_t *)arg;
    SharedData    *shm = m->shm;
    sem_t         *sem_mutex = m->sem_mutex;
    char line[128];      /* one flight per line, stays under PIPE_BUF */
    int  n;
    int  running;
    int  total;
    shm_flight_t snap[MAX_FLIGHTS];

    for(;;){
        while(sem_wait(sem_mutex) == -1 && errno == EINTR)
            ;
        running = shm->simulation_running;
        total   = shm->total_flights;
        if(total > MAX_FLIGHTS)
            total = MAX_FLIGHTS;
        memcpy(snap, shm->flights, (size_t)total * sizeof(shm_flight_t));
        sem_post(sem_mutex);

        if(!running)
            break;

        twrite("[Monitor] Estado dos voos em shm:\n");
        /* one write per voo so a big snapshot cant overflow the buffer */
        for(int i = 0; i < total; i++){
            shm_flight_t *f = &snap[i];
            n = snprintf(line, sizeof(line),
                         "  Voo %2d: step=%3d pos=(%.1f, %.1f, alt=%.1f) %s\n",
                         f->flight_id, f->step, f->x, f->y, f->alt,
                         f->active ? "ATIVO" : "INATIVO");
            if(n < 0)
                continue;
            if(n > (int)sizeof(line))
                n = sizeof(line);   /* snprintf truncated, dont read past it */
            write(STDOUT_FILENO, line, (size_t)n);
        }
        sleep(1);
    }

    twrite("[Monitor] Monitorização terminada.\n");
    return NULL;
}

/* US105 AC1+AC5 — thread de controlo: condition variable, aguarda todos os voos */
static void *control_thread(void *arg){
    (void)arg;

    pthread_mutex_lock(&g_mutex);
    while(g_flights_done < g_total_flights)      /* SEMPRE while — spurious wakeups */
        pthread_cond_wait(&g_cond, &g_mutex);
    pthread_mutex_unlock(&g_mutex);

    twrite("[Control] Todos os voos concluidos. Simulação terminada.\n");
    return NULL;
}

static sim_result_t main_loop(pid_t pids[], const sim_params_t *params,
                              int pos_fd_read, detection_context_t *det_ctx,
                              sem_t *sem_tick[], sem_t *sem_done){
    int step;
    int i;
    int received;
    sim_result_t result;

    (void)pids;
    result.violation_count = 0;
    result.completed_steps = 0;
    result.end_reason = SIM_END_NORMAL;

    for(step=0; step<params->total_steps; step++){
        position_update_t latest[MAX_FLIGHTS];

        memset(latest, 0, sizeof(latest));

        /* US108 AC1: open this step by posting each flight's own tick. one
         * post wakes exactly that flight, so no flight can grab two and
         * starve another (PL5 ex2). Replaces the Sprint-2 SIGUSR2 tick. */
        for(i=0; i<params->n_flights; i++)
            sem_post(sem_tick[i]);

        /* Drain n_flights updates this step. This still coordinates the step
           completion, while US106 detection scans the authoritative shm. */
        for(received=0; received<params->n_flights; received++){
            position_update_t update;
            int read_result;

            read_result = read_full(pos_fd_read, &update,
                                    sizeof(position_update_t));
            if(read_result <= 0){
                perror("read position update");
                result.end_reason = SIM_END_POSITION_READ_ERROR;
                return result;
            }
            if(update.flight_index < 0 ||
               update.flight_index >= params->n_flights){
                fprintf(stderr, "Invalid flight index in update: %d\n",
                        update.flight_index);
                result.end_reason = SIM_END_POSITION_READ_ERROR;
                return result;
            }
            latest[update.flight_index] = update;
        }

        /* US108 AC2: semaphore barrier — collect one sem_done per flight.
         * The parent cannot reach this point's end until every flight has
         * posted sem_done for this step, guaranteeing all flights finished
         * step `step` (and wrote their shm state) before the detection thread
         * scans shm and before step+1 begins (AC3: consistent per-step view). */
        for(i=0; i<params->n_flights; i++)
            sem_wait(sem_done);

        for(i=0; i<params->n_flights; i++){
            fprintf(stderr,
                    "step %d: flight %d at (%.1f, %.1f, alt %.1f)\n",
                    step,
                    latest[i].flight_id,
                    latest[i].x, latest[i].y, latest[i].alt);
        }

        pthread_mutex_lock(&det_ctx->mutex);
        det_ctx->current_step = step;
        det_ctx->step_processed = 0;
        det_ctx->step_ready = 1;
        pthread_cond_signal(&det_ctx->step_ready_cond);
        while(!det_ctx->step_processed)
            pthread_cond_wait(&det_ctx->step_done_cond, &det_ctx->mutex);

        result.completed_steps = step + 1;
        result.violation_count = det_ctx->violation_count;
        if(det_ctx->threshold_exceeded){
            result.end_reason = SIM_END_THRESHOLD_EXCEEDED;
            pthread_mutex_unlock(&det_ctx->mutex);
            break;
        }
        pthread_mutex_unlock(&det_ctx->mutex);
    }

    return result;
}

static int fork_flights(pid_t pids[], flight_t flights[],
                        const sim_params_t *params, int pos_fd[2]){
    int i;

    for(i=0; i<params->n_flights; i++){
        pids[i] = fork();
        if(pids[i] == -1){
            perror("fork flight");
            return -1;
        }

        if(pids[i] == 0){
            close(pos_fd[0]);
            flight_process(&flights[i], pos_fd[1], params->total_steps,
                           params->seconds_per_step);
            /* flight_process never returns; every path ends in _exit() */
        }
    }

    return 0;
}

int main(int argc, char *argv[]){
    pid_t pids[MAX_FLIGHTS];
    int pos_fd[2];
    int i;
    const char *path;
    sim_params_t params;
    flight_t flights[MAX_FLIGHTS];
    sim_result_t result;
    sigset_t tick_mask;
    sigset_t old_mask;
    int main_status;
    /* US105 */
    int         shm_fd;
    SharedData *shm;
    sem_t      *sem_mutex;
    sem_t      *sem_barrier;
    sem_t      *sem_nproc;
    sem_t      *sem_tick[MAX_FLIGHTS]; /* US108: one gate per flight */
    sem_t      *sem_done;    /* US108: per-step ack  (flights -> parent) */
    char        tick_name[SEM_TICK_NAME_LEN];
    pthread_t   t_monitor;
    pthread_t   t_control;
    monitor_arg_t monitor_arg;
    env_thread_arg_t env_arg;
    /* US106 */
    report_queue_t report_queue;
    report_thread_arg_t report_arg;
    detection_context_t det_ctx;
    pthread_t t_report;
    pthread_t t_detection;
    pthread_t t_env;

    path = argc > 1 ? argv[1] : "clean.json";

    if(load_config_from_json(path, &params, flights) != 0)
        return 1;

    if(validate_config(&params, flights) != 0)
        return 1;

    shm_unlink(SHM_NAME);
    sem_unlink(SEM_MUTEX);
    sem_unlink(SEM_BARRIER);
    sem_unlink(SEM_NPROC);
    sem_unlink(SEM_DONE);
    /* US108: clear any leftover per-flight ticks from a crashed run */
    for(i = 0; i < params.n_flights; i++){
        snprintf(tick_name, sizeof(tick_name), "%s%d", SEM_TICK_PREFIX, i);
        sem_unlink(tick_name);
    }

    shm_fd = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, S_IRUSR | S_IWUSR);
    if(shm_fd == -1){ perror("shm_open"); return 1; }
    if(ftruncate(shm_fd, sizeof(SharedData)) == -1){ perror("ftruncate"); return 1; }
    shm = mmap(NULL, sizeof(SharedData), PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
    if(shm == MAP_FAILED){ perror("mmap"); return 1; }

    memset(shm, 0, sizeof(SharedData));
    shm->total_flights      = params.n_flights;
    shm->simulation_running = 1;
    for(i = 0; i < params.n_flights; i++){
        shm->flights[i].flight_id = flights[i].id;
        shm->flights[i].active    = (flights[i].entry_step == 0) ? 1 : 0;
        shm->flights[i].x         = flights[i].x;
        shm->flights[i].y         = flights[i].y;
        shm->flights[i].alt       = flights[i].alt;
        shm->flights[i].prev_x    = flights[i].x;
        shm->flights[i].prev_y    = flights[i].y;
        shm->flights[i].prev_alt  = flights[i].alt;
        shm->flights[i].vx        = flights[i].vx;
        shm->flights[i].vy        = flights[i].vy;
        shm->flights[i].vz        = flights[i].vz;
    }

    sem_mutex   = sem_open(SEM_MUTEX,   O_CREAT | O_EXCL, 0644, 1);
    sem_barrier = sem_open(SEM_BARRIER, O_CREAT | O_EXCL, 0644, 0);
    sem_nproc   = sem_open(SEM_NPROC,   O_CREAT | O_EXCL, 0644, 1);
    /* US108: step semaphores start at 0 (no flight may run until the parent
     * posts its tick; the parent blocks on sem_done until flights ack). */
    sem_done    = sem_open(SEM_DONE,    O_CREAT | O_EXCL, 0644, 0);
    if(sem_mutex == SEM_FAILED || sem_barrier == SEM_FAILED || sem_nproc == SEM_FAILED ||
       sem_done == SEM_FAILED){
        perror("sem_open"); return 1;
    }
    /* one tick per flight (init 0) */
    for(i = 0; i < params.n_flights; i++){
        snprintf(tick_name, sizeof(tick_name), "%s%d", SEM_TICK_PREFIX, i);
        sem_tick[i] = sem_open(tick_name, O_CREAT | O_EXCL, 0644, 0);
        if(sem_tick[i] == SEM_FAILED){ perror("sem_open tick"); return 1; }
    }

    g_total_flights = params.n_flights;
    monitor_arg.shm = shm;
    monitor_arg.sem_mutex = sem_mutex;
    env_arg.shm = shm;
    env_arg.sem_mutex = sem_mutex;
    pthread_create(&t_monitor, NULL, monitor_thread, &monitor_arg);
    pthread_create(&t_control, NULL, control_thread, NULL);
    pthread_create(&t_env, NULL, environment_thread, &env_arg);


    if(pipe(pos_fd) == -1){
        perror("pipe positions");
        return 1;
    }

    sigemptyset(&tick_mask);
    sigaddset(&tick_mask, SIGUSR2);
    if(sigprocmask(SIG_BLOCK, &tick_mask, &old_mask) == -1){
        perror("sigprocmask");
        close(pos_fd[0]);
        close(pos_fd[1]);
        return 1;
    }

    if(fork_flights(pids, flights, &params, pos_fd) != 0){
        close(pos_fd[0]);
        close(pos_fd[1]);
        return 1;
    }

    if(sigprocmask(SIG_SETMASK, &old_mask, NULL) == -1)
        perror("sigprocmask restore");

    close(pos_fd[1]);

    if(report_queue_init(&report_queue) != 0){
        perror("report_queue_init");
        return 1;
    }

    report_arg.queue = &report_queue;
    report_arg.report_path = "report.txt";
    report_arg.violations_path = "violations.log";
    if(pthread_create(&t_report, NULL, report_generation_thread, &report_arg) != 0){
        perror("pthread_create report_generation_thread");
        return 1;
    }

    if(detection_context_init(&det_ctx, shm, sem_mutex, pids, &params,
                              &report_queue) != 0){
        perror("detection_context_init");
        report_queue_finish(&report_queue);
        pthread_join(t_report, NULL);
        return 1;
    }
    if(pthread_create(&t_detection, NULL, violation_detection_thread,
                      &det_ctx) != 0){
        perror("pthread_create violation_detection_thread");
        detection_context_destroy(&det_ctx);
        report_queue_finish(&report_queue);
        pthread_join(t_report, NULL);
        return 1;
    }

    enqueue_metadata(&report_queue, &params);

    result = main_loop(pids, &params, pos_fd[0], &det_ctx, sem_tick, sem_done);
    close(pos_fd[0]);

    if(result.end_reason == SIM_END_POSITION_READ_ERROR){
        for(i=0; i<params.n_flights; i++)
            kill(pids[i], SIGTERM);
    }

    /* US108: sim is over. Set the stop flag, then poke each flight's tick once
     * so any flight thats waiting wakes up, sees the flag and quits. */
    sem_wait(sem_mutex);
    shm->simulation_running = 0;
    sem_post(sem_mutex);
    for(i=0; i<params.n_flights; i++)
        sem_post(sem_tick[i]);

    detection_context_stop(&det_ctx);
    pthread_join(t_detection, NULL);

    enqueue_sim_end(&report_queue, &result);
    enqueue_flight_statuses(&report_queue, pids, flights, params.n_flights,
                            result.end_reason);

    report_queue_finish(&report_queue);
    pthread_join(t_report, NULL);

    detection_context_destroy(&det_ctx);
    report_queue_destroy(&report_queue);

    pthread_mutex_lock(&g_mutex);
    g_flights_done = params.n_flights;
    pthread_cond_broadcast(&g_cond);
    pthread_mutex_unlock(&g_mutex);

    pthread_join(t_monitor, NULL);
    pthread_join(t_control, NULL);
    pthread_join(t_env, NULL);


    munmap(shm, sizeof(SharedData));
    close(shm_fd);
    shm_unlink(SHM_NAME);

    sem_close(sem_mutex);
    sem_close(sem_barrier);
    sem_close(sem_nproc);
    sem_close(sem_done);
    sem_unlink(SEM_MUTEX);
    sem_unlink(SEM_BARRIER);
    sem_unlink(SEM_NPROC);
    sem_unlink(SEM_DONE);
    /* close and unlink each per-flight tick */
    for(i=0; i<params.n_flights; i++){
        sem_close(sem_tick[i]);
        snprintf(tick_name, sizeof(tick_name), "%s%d", SEM_TICK_PREFIX, i);
        sem_unlink(tick_name);
    }

    pthread_mutex_destroy(&g_mutex);
    pthread_cond_destroy(&g_cond);

    main_status = result.end_reason == SIM_END_POSITION_READ_ERROR ? 1 : 0;
    return main_status;
}
