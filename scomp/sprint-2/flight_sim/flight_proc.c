#include"types.h"

#include<errno.h>
#include<signal.h>
#include<stdlib.h>
#include<string.h>
#include<unistd.h>

static volatile sig_atomic_t tick = 0;
static volatile sig_atomic_t time_to_go = 0;

static void handle_tick(int sig){
    (void)sig;
    tick = 1;
}

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
    sigfillset(&act.sa_mask);

    act.sa_handler = handle_tick;
    if(sigaction(SIGUSR2, &act, NULL) == -1)
        _exit(FLIGHT_EXIT_WRITE_ERROR);

    act.sa_handler = handle_violation;
    if(sigaction(SIGUSR1, &act, NULL) == -1)
        _exit(FLIGHT_EXIT_WRITE_ERROR);

    act.sa_handler = handle_term;
    if(sigaction(SIGTERM, &act, NULL) == -1)
        _exit(FLIGHT_EXIT_WRITE_ERROR);
}

void flight_process(flight_t *flight, int write_fd, int total_steps){
    int step;
    sigset_t empty_mask;
    position_update_t update;

    install_handlers();
    sigemptyset(&empty_mask);

    for(step=0; step<total_steps && !time_to_go; step++){
        while(!tick && !time_to_go)
            sigsuspend(&empty_mask);

        if(time_to_go)
            break;

        tick = 0;

        flight->x += flight->vx;
        flight->y += flight->vy;
        flight->alt += flight->vz;

        update.flight_index = flight->index;
        update.flight_id = flight->id;
        update.step = step;
        update.x = flight->x;
        update.y = flight->y;
        update.alt = flight->alt;
        update.vx = flight->vx;
        update.vy = flight->vy;
        update.vz = flight->vz;

        if(write_full(write_fd, &update, sizeof(position_update_t)) != 0){
            close(write_fd);
            _exit(FLIGHT_EXIT_WRITE_ERROR);
        }
    }

    close(write_fd);
    if(time_to_go)
        _exit(FLIGHT_EXIT_TERMINATED);

    _exit(FLIGHT_EXIT_COMPLETED);
}
