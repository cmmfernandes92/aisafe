#ifndef FLIGHT_SIM_TYPES_H
#define FLIGHT_SIM_TYPES_H

#define _POSIX_C_SOURCE 200809L

#include<sys/types.h>
#include<stdio.h>
#include<signal.h>

#define MAX_FLIGHTS 50
#define HISTORY_SIZE 600
#define SCENARIO_NAME_SIZE 32
#define RUN_TIMESTAMP_SIZE 32

#define H_MIN 8.0
#define V_MIN 600.0

#define FLIGHT_EXIT_COMPLETED 0
#define FLIGHT_EXIT_TERMINATED 2
#define FLIGHT_EXIT_WRITE_ERROR 3

typedef struct{
    int    index;
    int    id;
    double x;
    double y;
    double alt;
    double vx;
    double vy;
    double vz;
}flight_t;

typedef struct{
    int    n_flights;
    int    total_steps;
    int    max_violations;
    double area_x_min;
    double area_x_max;
    double area_y_min;
    double area_y_max;
    double area_alt_min;
    double area_alt_max;
    double h_min;
    double v_min;
    char   scenario[SCENARIO_NAME_SIZE];
}sim_params_t;

typedef struct{
    int    flight_index;
    int    flight_id;
    int    step;
    double x;
    double y;
    double alt;
    double vx;
    double vy;
    double vz;
}position_update_t;

typedef enum{
    SIM_END_NORMAL,
    SIM_END_THRESHOLD_EXCEEDED,
    SIM_END_POSITION_READ_ERROR
}sim_end_reason_t;

typedef enum{
    FLIGHT_COMPLETED,
    FLIGHT_ABORTED,
    FLIGHT_TERMINATED_DUE_TO_VIOLATION,
    FLIGHT_CRASHED
}flight_outcome_t;

typedef enum{
    EV_METADATA,
    EV_VIOLATION,
    EV_FLIGHT_STATUS,
    EV_SIM_END
}event_kind_t;

typedef struct{
    event_kind_t kind;
    union{
        struct{
            int    total_steps;
            int    n_flights;
            int    max_violations;
            double h_min;
            double v_min;
            char   scenario[SCENARIO_NAME_SIZE];
            char   run_timestamp[RUN_TIMESTAMP_SIZE];
        }meta;
        struct{
            int    step;
            int    f1_id;
            int    f2_id;
            double h_dist;
            double v_dist;
            double x1;
            double y1;
            double alt1;
            double vx1;
            double vy1;
            double vz1;
            double x2;
            double y2;
            double alt2;
            double vx2;
            double vy2;
            double vz2;
        }violation;
        struct{
            int              flight_id;
            int              exit_code;
            flight_outcome_t outcome;
        }status;
        struct{
            sim_end_reason_t reason;
            int              completed_steps;
            int              violation_count;
        }end;
    }data;
}report_event_t;

typedef struct{
    int              violation_count;
    int              completed_steps;
    sim_end_reason_t end_reason;
}sim_result_t;

int load_config_from_json(const char *scenario, sim_params_t *params,
                          flight_t flights[]);
int validate_config(const sim_params_t *params, const flight_t flights[]);

void flight_process(flight_t *flight, int write_fd, int total_steps);

sim_result_t main_loop(pid_t pids[], const sim_params_t *params,
                       int pos_fd_read, int report_fd_write);
void check_violations(position_update_t latest[], int n_flights,
                      pid_t pids[], int *violation_count,
                      int max_violations, double h_min, double v_min,
                      FILE *log,
                      int notified[MAX_FLIGHTS][MAX_FLIGHTS],
                      int report_fd_write);

void report_process(int read_fd, const char *path);
int write_report_event(int fd, const report_event_t *event);
int write_full(int fd, const void *buffer, size_t size);
int read_full(int fd, void *buffer, size_t size);

const char *flight_outcome_name(flight_outcome_t outcome);
const char *sim_end_reason_name(sim_end_reason_t reason);

#endif
