#ifndef DETECTION_THREAD_H
#define DETECTION_THREAD_H

#include "types.h"
#include "report_thread.h"

typedef struct{
    SharedData *shm;
    sem_t *sem_mutex;
    pid_t *pids;
    int n_flights;
    int max_violations;
    double h_min;
    double v_min;
    double area_alt_min;   /* airspace floor: alt below this is a VIOL_ALT_FLOOR  */
    double area_alt_max;   /* airspace ceiling: alt above this is a VIOL_ALT_CEILING */
    int lookahead;         /* forward horizon (steps) for predictive conflict warnings */
    report_queue_t *report_queue;

    pthread_mutex_t mutex;
    pthread_cond_t step_ready_cond;
    pthread_cond_t step_done_cond;
    int step_ready;
    int step_processed;
    int current_step;
    int simulation_done;
    int violation_count;
    int threshold_exceeded;
    int notified[MAX_FLIGHTS][MAX_FLIGHTS];
    int alt_notified[MAX_FLIGHTS]; 
} detection_context_t;

int detection_context_init(detection_context_t *ctx, SharedData *shm,
                           sem_t *sem_mutex, pid_t pids[],
                           const sim_params_t *params,
                           report_queue_t *report_queue);
void detection_context_destroy(detection_context_t *ctx);
void detection_context_stop(detection_context_t *ctx);
void *violation_detection_thread(void *arg);

#endif
