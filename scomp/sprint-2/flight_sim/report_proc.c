#include"types.h"

#include<errno.h>
#include<stdlib.h>
#include<string.h>
#include<unistd.h>

int write_full(int fd, const void *buffer, size_t size){
    const char *current;
    size_t written_total;

    current = (const char *)buffer;
    written_total = 0;

    while(written_total < size){
        ssize_t written;

        written = write(fd, current + written_total, size - written_total);
        if(written == -1){
            if(errno == EINTR)
                continue;
            return -1;
        }
        if(written == 0)
            return -1;

        written_total += (size_t)written;
    }

    return 0;
}

int read_full(int fd, void *buffer, size_t size){
    char *current;
    size_t read_total;

    current = (char *)buffer;
    read_total = 0;

    while(read_total < size){
        ssize_t bytes_read;

        bytes_read = read(fd, current + read_total, size - read_total);
        if(bytes_read == -1){
            if(errno == EINTR)
                continue;
            return -1;
        }
        if(bytes_read == 0){
            if(read_total == 0)
                return 0;
            return -1;
        }

        read_total += (size_t)bytes_read;
    }

    return 1;
}

int write_report_event(int fd, const report_event_t *event){
    return write_full(fd, event, sizeof(report_event_t));
}

const char *flight_outcome_name(flight_outcome_t outcome){
    switch(outcome){
        case FLIGHT_COMPLETED:
            return "completed";
        case FLIGHT_ABORTED:
            return "aborted";
        case FLIGHT_TERMINATED_DUE_TO_VIOLATION:
            return "terminated_due_to_violation";
        case FLIGHT_CRASHED:
            return "crashed";
        default:
            return "unknown";
    }
}

const char *sim_end_reason_name(sim_end_reason_t reason){
    switch(reason){
        case SIM_END_NORMAL:
            return "normal";
        case SIM_END_THRESHOLD_EXCEEDED:
            return "threshold_exceeded";
        case SIM_END_POSITION_READ_ERROR:
            return "position_read_error";
        default:
            return "unknown";
    }
}

static int grow_events(report_event_t **events, int *capacity){
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

static int append_event(report_event_t **events, int *count, int *capacity,
                        const report_event_t *event){
    if(*count >= *capacity){
        if(grow_events(events, capacity) != 0)
            return -1;
    }

    (*events)[*count] = *event;
    (*count)++;
    return 0;
}

static void write_report(FILE *fp, const report_event_t *meta,
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
            "x1,y1,alt1,vx1,vy1,vz1,x2,y2,alt2,vx2,vy2,vz2\n");
    for(i=0; i<n_violations; i++){
        const report_event_t *event;

        event = &violations[i];
        fprintf(fp,
                "%d,%d,%d,%.2f,%.2f,"
                "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,"
                "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
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
                event->data.violation.vz2);
    }
}

void report_process(int read_fd, const char *path){
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
    int read_result;
    FILE *fp;

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

    while((read_result = read_full(read_fd, &event,
                                   sizeof(report_event_t))) > 0){
        if(event.kind == EV_METADATA){
            meta = event;
            have_meta = 1;
        }
        else if(event.kind == EV_VIOLATION){
            if(append_event(&violations, &n_violations, &cap_violations,
                            &event) != 0){
                perror("realloc violations");
                free(violations);
                free(statuses);
                exit(1);
            }
        }
        else if(event.kind == EV_FLIGHT_STATUS){
            if(append_event(&statuses, &n_status, &cap_statuses,
                            &event) != 0){
                perror("realloc statuses");
                free(violations);
                free(statuses);
                exit(1);
            }
        }
        else if(event.kind == EV_SIM_END){
            end = event;
            have_end = 1;
        }
    }

    if(read_result < 0)
        perror("read report pipe");

    fp = fopen(path, "w");
    if(fp == NULL){
        perror("fopen report");
        free(violations);
        free(statuses);
        exit(1);
    }

    write_report(fp, &meta, have_meta, violations, n_violations, statuses,
                 n_status, &end, have_end);
    fclose(fp);
    free(violations);
    free(statuses);
}
