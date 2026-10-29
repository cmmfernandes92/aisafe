#include "report_thread.h"

#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

static void build_run_timestamp(char output[RUN_TIMESTAMP_SIZE]){
    time_t now;
    struct tm tm_now;

    now = time(NULL);
    if(localtime_r(&now, &tm_now) == NULL){
        strncpy(output, "unknown", RUN_TIMESTAMP_SIZE - 1);
        output[RUN_TIMESTAMP_SIZE - 1] = '\0';
        return;
    }

    strftime(output, RUN_TIMESTAMP_SIZE, "%Y-%m-%dT%H:%M:%S", &tm_now);
}

int report_queue_init(report_queue_t *queue){
    memset(queue, 0, sizeof(*queue));
    if(pthread_mutex_init(&queue->mutex, NULL) != 0)
        return -1;
    if(pthread_cond_init(&queue->cond, NULL) != 0){
        pthread_mutex_destroy(&queue->mutex);
        return -1;
    }
    return 0;
}

void report_queue_destroy(report_queue_t *queue){
    report_node_t *node;

    node = queue->head;
    while(node != NULL){
        report_node_t *next = node->next;
        free(node);
        node = next;
    }
    pthread_cond_destroy(&queue->cond);
    pthread_mutex_destroy(&queue->mutex);
}

int enqueue_report_event(report_queue_t *queue, const report_event_t *event){
    report_node_t *node;

    node = malloc(sizeof(report_node_t));
    if(node == NULL)
        return -1;
    node->event = *event;
    node->next = NULL;

    /* US107 AC2+AC4: mutate shared state (append to queue) BEFORE signalling,
     * both under the mutex.  This is the "publish then signal" rule that
     * prevents lost wakeups: even if the report thread is not yet waiting on
     * the condition variable, the node is already in the queue, so when the
     * thread eventually calls pthread_cond_wait it will retest the predicate
     * (while(head==NULL && !done)) find it false, and skip the wait. */
    pthread_mutex_lock(&queue->mutex);
    if(queue->tail == NULL)
        queue->head = node;
    else
        queue->tail->next = node;
    queue->tail = node;
    pthread_cond_signal(&queue->cond);   /* US107 AC2: signal under mutex */
    pthread_mutex_unlock(&queue->mutex);

    return 0;
}

static int dequeue_report_event(report_queue_t *queue, report_event_t *event){
    report_node_t *node;

    /* US107 AC3+AC4: the report thread blocks here until the detection thread
     * (or main) signals queue->cond. The while() loop (not if) re-tests the
     * predicate after every wakeup, guarding against both spurious wakeups and
     * the case where multiple consumers drain the queue between signal and
     * mutex re-acquisition. pthread_cond_wait atomically releases the mutex
     * and suspends — no lost-wakeup possible when combined with the
     * "enqueue then signal" order in enqueue_report_event. */
    pthread_mutex_lock(&queue->mutex);
    while(queue->head == NULL && !queue->done)
        pthread_cond_wait(&queue->cond, &queue->mutex);

    if(queue->head == NULL && queue->done){
        pthread_mutex_unlock(&queue->mutex);
        return 0;
    }

    node = queue->head;
    queue->head = node->next;
    if(queue->head == NULL)
        queue->tail = NULL;
    pthread_mutex_unlock(&queue->mutex);

    *event = node->event;
    free(node);
    return 1;
}

void report_queue_finish(report_queue_t *queue){
    pthread_mutex_lock(&queue->mutex);
    queue->done = 1;
    pthread_cond_broadcast(&queue->cond);
    pthread_mutex_unlock(&queue->mutex);
}

void enqueue_metadata(report_queue_t *queue, const sim_params_t *params){
    report_event_t event;

    memset(&event, 0, sizeof(report_event_t));
    event.kind = EV_METADATA;
    event.data.meta.total_steps = params->total_steps;
    event.data.meta.n_flights = params->n_flights;
    event.data.meta.max_violations = params->max_violations;
    event.data.meta.h_min = params->h_min;
    event.data.meta.v_min = params->v_min;
    event.data.meta.seconds_per_step = params->seconds_per_step;
    strncpy(event.data.meta.scenario, params->scenario,
            SCENARIO_NAME_SIZE - 1);
    build_run_timestamp(event.data.meta.run_timestamp);

    if(enqueue_report_event(queue, &event) != 0)
        perror("enqueue report metadata");
}

void enqueue_sim_end(report_queue_t *queue, const sim_result_t *result){
    report_event_t event;

    memset(&event, 0, sizeof(report_event_t));
    event.kind = EV_SIM_END;
    event.data.end.reason = result->end_reason;
    event.data.end.completed_steps = result->completed_steps;
    event.data.end.violation_count = result->violation_count;

    if(enqueue_report_event(queue, &event) != 0)
        perror("enqueue report end");
}

static flight_outcome_t map_status(int status_raw,
                                   sim_end_reason_t end_reason){
    if(WIFEXITED(status_raw)){
        int code;

        code = WEXITSTATUS(status_raw);
        if(code == FLIGHT_EXIT_COMPLETED)
            return FLIGHT_COMPLETED;
        if(code == FLIGHT_EXIT_TERMINATED)
            return FLIGHT_TERMINATED_DUE_TO_VIOLATION;
        return FLIGHT_ABORTED;
    }

    if(WIFSIGNALED(status_raw)){
        if(WTERMSIG(status_raw) == SIGTERM &&
           end_reason == SIM_END_THRESHOLD_EXCEEDED)
            return FLIGHT_TERMINATED_DUE_TO_VIOLATION;
        return FLIGHT_CRASHED;
    }

    return FLIGHT_CRASHED;
}

static int map_exit_code(int status_raw){
    if(WIFEXITED(status_raw))
        return WEXITSTATUS(status_raw);
    if(WIFSIGNALED(status_raw))
        return 128 + WTERMSIG(status_raw);
    return -1;
}

void enqueue_flight_statuses(report_queue_t *queue, pid_t pids[],
                                    flight_t flights[], int n_flights,
                                    sim_end_reason_t end_reason){
    int i;

    for(i=0; i<n_flights; i++){
        int status_raw;
        report_event_t event;

        if(waitpid(pids[i], &status_raw, 0) == -1){
            perror("waitpid flight");
            memset(&event, 0, sizeof(report_event_t));
            event.kind = EV_FLIGHT_STATUS;
            event.data.status.flight_id = flights[i].id;
            event.data.status.exit_code = -1;
            event.data.status.outcome = FLIGHT_CRASHED;
        }
        else{
            memset(&event, 0, sizeof(report_event_t));
            event.kind = EV_FLIGHT_STATUS;
            event.data.status.flight_id = flights[i].id;
            event.data.status.exit_code = map_exit_code(status_raw);
            event.data.status.outcome = map_status(status_raw, end_reason);
        }

        if(enqueue_report_event(queue, &event) != 0)
            perror("enqueue report status");
    }
}

static int grow_report_events(report_event_t **events, int *capacity){
    int new_capacity;
    report_event_t *new_events;

    new_capacity = *capacity == 0 ? 32 : *capacity * 2;
    new_events = realloc(*events, (size_t)new_capacity * sizeof(report_event_t));
    if(new_events == NULL)
        return -1;

    *events = new_events;
    *capacity = new_capacity;
    return 0;
}

static int append_report_event(report_event_t **events, int *count,
                               int *capacity, const report_event_t *event){
    if(*count >= *capacity){
        if(grow_report_events(events, capacity) != 0)
            return -1;
    }

    (*events)[*count] = *event;
    (*count)++;
    return 0;
}

static void write_report_file(FILE *fp, const report_event_t *meta,
                              int have_meta, const report_event_t violations[],
                              int n_violations, const report_event_t statuses[],
                              int n_status, const report_event_t *end,
                              int have_end){
    int i;
    int n_completed;
    int pass;
    int max_violations;

    n_completed = 0;
    for(i=0; i<n_status; i++){
        if(statuses[i].data.status.outcome == FLIGHT_COMPLETED)
            n_completed++;
    }

    max_violations = have_meta ? meta->data.meta.max_violations : 0;
    pass = have_end &&
           end->data.end.reason == SIM_END_NORMAL &&
           n_violations <= max_violations &&
           have_meta &&
           n_completed == meta->data.meta.n_flights;

    fprintf(fp, "report_version=1\n");
    if(have_meta){
        fprintf(fp, "run_timestamp=%s\n", meta->data.meta.run_timestamp);
        fprintf(fp, "scenario=%s\n", meta->data.meta.scenario);
        fprintf(fp, "total_steps=%d\n", meta->data.meta.total_steps);
        fprintf(fp, "total_flights=%d\n", meta->data.meta.n_flights);
        fprintf(fp, "max_violations=%d\n", meta->data.meta.max_violations);
        fprintf(fp, "horizontal_min=%.2f\n", meta->data.meta.h_min);
        fprintf(fp, "vertical_min=%.2f\n", meta->data.meta.v_min);
        fprintf(fp, "seconds_per_step=%.2f\n", meta->data.meta.seconds_per_step);
    }
    if(have_end){
        fprintf(fp, "completed_steps=%d\n", end->data.end.completed_steps);
        fprintf(fp, "end_reason=%s\n",
                sim_end_reason_name(end->data.end.reason));
        fprintf(fp, "reported_violation_count=%d\n",
                end->data.end.violation_count);
    }
    fprintf(fp, "stored_violation_events=%d\n", n_violations);
    fprintf(fp, "validation=%s\n", pass ? "PASS" : "FAIL");

    fprintf(fp, "\n[flight_statuses]\n");
    fprintf(fp, "flight_id,outcome,exit_code\n");
    for(i=0; i<n_status; i++){
        fprintf(fp, "%d,%s,%d\n",
                statuses[i].data.status.flight_id,
                flight_outcome_name(statuses[i].data.status.outcome),
                statuses[i].data.status.exit_code);
    }

    fprintf(fp, "\n[violations]\n");
    fprintf(fp,
            "step,flight_1,flight_2,h_dist,v_dist,"
            "x1,y1,alt1,vx1,vy1,vz1,x2,y2,alt2,vx2,vy2,vz2,kind\n");
    for(i=0; i<n_violations; i++){
        const report_event_t *event;

        event = &violations[i];
        fprintf(fp,
                "%d,%d,%d,%.2f,%.2f,"
                "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,"
                "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                event->data.violation.step,
                event->data.violation.f1_id,
                event->data.violation.f2_id,
                event->data.violation.h_dist,
                event->data.violation.v_dist,
                event->data.violation.x1,
                event->data.violation.y1,
                event->data.violation.alt1,
                event->data.violation.vx1,
                event->data.violation.vy1,
                event->data.violation.vz1,
                event->data.violation.x2,
                event->data.violation.y2,
                event->data.violation.alt2,
                event->data.violation.vx2,
                event->data.violation.vy2,
                event->data.violation.vz2,
                violation_kind_name(event->data.violation.viol_type));
    }
}

static void write_violation_log_entry(FILE *log, const report_event_t *event){
    fprintf(log,
            "step=%d flight_1=%d flight_2=%d h_dist=%.2f v_dist=%.2f\n",
            event->data.violation.step,
            event->data.violation.f1_id,
            event->data.violation.f2_id,
            event->data.violation.h_dist,
            event->data.violation.v_dist);
    fflush(log);
}

/* thread-safe error line: snprintf + one write() (printf-family is not atomic across threads). */
static void report_thread_perror(const char *msg){
    char buf[256];
    int n;

    n = snprintf(buf, sizeof(buf), "%s (errno %d)\n", msg, errno);
    if(n > 0)
        write(STDERR_FILENO, buf, (size_t)n);
}

void *report_generation_thread(void *arg){
    report_thread_arg_t *ctx;
    report_event_t event;
    report_event_t meta;
    report_event_t end;
    report_event_t *violations;
    report_event_t *statuses;
    int have_meta;
    int have_end;
    int n_violations;
    int n_status;
    int cap_violations;
    int cap_statuses;
    int dequeue_result;
    FILE *viol_log;
    FILE *report;

    ctx = (report_thread_arg_t *)arg;
    memset(&meta, 0, sizeof(report_event_t));
    memset(&end, 0, sizeof(report_event_t));
    violations = NULL;
    statuses = NULL;
    have_meta = 0;
    have_end = 0;
    n_violations = 0;
    n_status = 0;
    cap_violations = 0;
    cap_statuses = 0;

    viol_log = fopen(ctx->violations_path, "w");
    if(viol_log == NULL)
        report_thread_perror("fopen violations.log");

    while((dequeue_result = dequeue_report_event(ctx->queue, &event)) > 0){
        if(event.kind == EV_METADATA){
            meta = event;
            have_meta = 1;
        }
        else if(event.kind == EV_VIOLATION){
            if(append_report_event(&violations, &n_violations, &cap_violations,
                                   &event) != 0){
                report_thread_perror("realloc violations");
                break;
            }
            if(viol_log != NULL)
                write_violation_log_entry(viol_log, &event);
        }
        else if(event.kind == EV_FLIGHT_STATUS){
            if(append_report_event(&statuses, &n_status, &cap_statuses,
                                   &event) != 0){
                report_thread_perror("realloc statuses");
                break;
            }
        }
        else if(event.kind == EV_SIM_END){
            end = event;
            have_end = 1;
        }
    }

    if(viol_log != NULL)
        fclose(viol_log);

    report = fopen(ctx->report_path, "w");
    if(report == NULL)
        report_thread_perror("fopen report");
    else{
        write_report_file(report, &meta, have_meta, violations, n_violations,
                          statuses, n_status, &end, have_end);
        fclose(report);
    }

    free(violations);
    free(statuses);
    return NULL;
}

