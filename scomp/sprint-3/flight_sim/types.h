#ifndef FLIGHT_SIM_TYPES_H
#define FLIGHT_SIM_TYPES_H

#define _POSIX_C_SOURCE 200809L

#include<sys/types.h>
#include<stdio.h>
#include<signal.h>
#include<fcntl.h>
#include<sys/mman.h>
#include<semaphore.h>
#include<pthread.h>

#define MAX_FLIGHTS 512
#define HISTORY_SIZE 600
#define MAX_HISTORY 8
#define MAX_PROFILE 16
#define MAX_SEGMENTS 8
#define SCENARIO_NAME_SIZE 32
#define RUN_TIMESTAMP_SIZE 32

#define H_MIN 8.0
#define V_MIN 600.0

#define FLIGHT_EXIT_COMPLETED 0
#define FLIGHT_EXIT_TERMINATED 2
#define FLIGHT_EXIT_WRITE_ERROR 3

/* Kinds of safety violation carried in the [violations] report rows. A
 * separation event involves a pair of flights; an altitude event is single-flight. */
#define VIOL_SEPARATION 0
#define VIOL_ALT_CEILING 1
#define VIOL_ALT_FLOOR 2

/* One altitude band of a climb/descend profile (raw from JSON). */
typedef struct{
    double altitude;
    double speed_knots;
    double rate;
}profile_point_t;

/*
 * One leg of a flight path.
 */
typedef struct{
    double start_lat;
    double start_lon;
    double start_alt;
    double end_lat;
    double end_lon;
    double end_alt;
    double start_x;
    double start_y;
    double end_x;
    double end_y;
}segment_t;

typedef struct{
    int    index;
    int    id;
    double x;       /* projected metres (east) */
    double y;       /* projected metres (north) */
    double alt;
    double vx;      /* derived per-step velocity (metres/step) */
    double vy;
    double vz;
    profile_point_t climb[MAX_PROFILE];
    int    n_climb;
    profile_point_t descend[MAX_PROFILE];
    int    n_descend;
    segment_t segments[MAX_SEGMENTS];
    int    n_segments;
    int    seg_index;
    int    entry_step;  /* step at which the flight enters the area (0 = active from start) */
    int    arrived;     /* 1 once the flight reaches its final step ( holds and goes inactive) */
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
    double seconds_per_step;
    int    history_depth;
    double ref_lat;
    double ref_lon;
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
            double seconds_per_step;
            char   scenario[SCENARIO_NAME_SIZE];
            char   run_timestamp[RUN_TIMESTAMP_SIZE];
        }meta;
        struct{
            int    viol_type;   /* VIOL_SEPARATION | VIOL_ALT_CEILING | VIOL_ALT_FLOOR */
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

typedef struct {
    float wind_speed;
    float wind_direction;
} env_t;

/* ── US105: Shared memory + semaphore names ── */
#define SHM_NAME     "/aisafe_shm"
#define SEM_MUTEX    "/aisafe_sem_mutex"
#define SEM_BARRIER  "/aisafe_sem_barrier"
#define SEM_NPROC    "/aisafe_sem_nproc"

/* ── US108: per-step lockstep semaphores ── */
/* one tick per flight so a post wakes exactly that flight */
#define SEM_TICK_PREFIX "/aisafe_sem_tick_"
#define SEM_TICK_NAME_LEN 32
#define SEM_DONE     "/aisafe_sem_done"

typedef struct {
    int    flight_id;
    int    step;
    double x;
    double y;
    double alt;
    double prev_x;
    double prev_y;
    double prev_alt;
    double vx;
    double vy;
    double vz;
    int    active;       /* 1 = voo ativo, 0 = terminado */
} shm_flight_t;

typedef struct {
    shm_flight_t flights[MAX_FLIGHTS]; /* posições em tempo real de cada voo */
    int          total_flights;
    int          nproc_at_barrier;     /* contador para barreira de sincronização */
    int          simulation_running;   /* 0 quando a simulação termina */

    env_t env;  /*novo campo*/
} SharedData;

int load_config_from_json(const char *scenario, sim_params_t *params,
                          flight_t flights[]);
int validate_config(const sim_params_t *params, const flight_t flights[]);

void flight_process(flight_t *flight, int write_fd, int total_steps,
                    double dt);

void check_violations(const position_update_t prev[],
                      const position_update_t cur[], int n_flights,
                      int has_prev,
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
const char *violation_kind_name(int viol_type);

#endif
