#ifndef REPORT_THREAD_H
#define REPORT_THREAD_H

#include "types.h"

typedef struct report_node{
    report_event_t event;
    struct report_node *next;
} report_node_t;

typedef struct{
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    report_node_t *head;
    report_node_t *tail;
    int done;
} report_queue_t;

typedef struct{
    report_queue_t *queue;
    const char *report_path;
    const char *violations_path;
} report_thread_arg_t;

int report_queue_init(report_queue_t *queue);
void report_queue_destroy(report_queue_t *queue);
int enqueue_report_event(report_queue_t *queue, const report_event_t *event);
void report_queue_finish(report_queue_t *queue);
void enqueue_metadata(report_queue_t *queue, const sim_params_t *params);
void enqueue_sim_end(report_queue_t *queue, const sim_result_t *result);
void enqueue_flight_statuses(report_queue_t *queue, pid_t pids[],
                             flight_t flights[], int n_flights,
                             sim_end_reason_t end_reason);
void *report_generation_thread(void *arg);

#endif
