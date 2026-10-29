#include"types.h"

#include<errno.h>
#include<signal.h>
#include<stdlib.h>
#include<string.h>
#include<sys/wait.h>
#include<time.h>
#include<unistd.h>

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

static void send_metadata(int report_fd_write, const sim_params_t *params){
    report_event_t event;

    memset(&event, 0, sizeof(report_event_t));
    event.kind = EV_METADATA;
    event.data.meta.total_steps = params->total_steps;
    event.data.meta.n_flights = params->n_flights;
    event.data.meta.max_violations = params->max_violations;
    event.data.meta.h_min = params->h_min;
    event.data.meta.v_min = params->v_min;
    strncpy(event.data.meta.scenario, params->scenario,
            SCENARIO_NAME_SIZE - 1);
    build_run_timestamp(event.data.meta.run_timestamp);

    if(write_report_event(report_fd_write, &event) != 0)
        perror("write report metadata");
}

static void send_sim_end(int report_fd_write, const sim_result_t *result){
    report_event_t event;

    memset(&event, 0, sizeof(report_event_t));
    event.kind = EV_SIM_END;
    event.data.end.reason = result->end_reason;
    event.data.end.completed_steps = result->completed_steps;
    event.data.end.violation_count = result->violation_count;

    if(write_report_event(report_fd_write, &event) != 0)
        perror("write report end");
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

static void send_flight_statuses(int report_fd_write, pid_t pids[],
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

        if(write_report_event(report_fd_write, &event) != 0)
            perror("write report status");
    }
}

sim_result_t main_loop(pid_t pids[], const sim_params_t *params,
                       int pos_fd_read, int report_fd_write){
    int step;
    int i;
    int violation_count;
    int notified[MAX_FLIGHTS][MAX_FLIGHTS];
    position_update_t latest[MAX_FLIGHTS];
    position_update_t history[MAX_FLIGHTS][HISTORY_SIZE];
    FILE *log;
    sim_result_t result;

    memset(notified, 0, sizeof(notified));
    memset(latest, 0, sizeof(latest));
    memset(history, 0, sizeof(history));

    violation_count = 0;
    result.violation_count = 0;
    result.completed_steps = 0;
    result.end_reason = SIM_END_NORMAL;

    log = fopen("violations.log", "w");
    if(log == NULL){
        perror("fopen violations.log");
        result.end_reason = SIM_END_POSITION_READ_ERROR;
        return result;
    }

    for(step=0; step<params->total_steps; step++){
        for(i=0; i<params->n_flights; i++){
            if(kill(pids[i], SIGUSR2) == -1)
                perror("kill SIGUSR2");
        }

        for(i=0; i<params->n_flights; i++){
            position_update_t update;
            int read_result;

            read_result = read_full(pos_fd_read, &update,
                                    sizeof(position_update_t));
            if(read_result <= 0){
                perror("read position update");
                result.end_reason = SIM_END_POSITION_READ_ERROR;
                fclose(log);
                return result;
            }
            if(update.flight_index < 0 ||
               update.flight_index >= params->n_flights){
                fprintf(stderr, "Invalid flight index in update: %d\n",
                        update.flight_index);
                result.end_reason = SIM_END_POSITION_READ_ERROR;
                fclose(log);
                return result;
            }

            latest[update.flight_index] = update;
            history[update.flight_index][update.step] = update;
        }

        check_violations(latest, params->n_flights, pids,
                         &violation_count, params->max_violations,
                         params->h_min, params->v_min, log,
                         notified, report_fd_write);

        result.completed_steps = step + 1;
        result.violation_count = violation_count;
        if(violation_count > params->max_violations){
            result.end_reason = SIM_END_THRESHOLD_EXCEEDED;
            break;
        }
    }

    fclose(log);
    return result;
}

static pid_t fork_report_process(int report_fd[2], int pos_fd[2]){
    pid_t report_pid;

    report_pid = fork();
    if(report_pid == -1){
        perror("fork report");
        return -1;
    }

    if(report_pid == 0){
        close(pos_fd[0]);
        close(pos_fd[1]);
        close(report_fd[1]);
        report_process(report_fd[0], "report.txt");
        close(report_fd[0]);
        _exit(0);
    }

    return report_pid;
}

static int fork_flights(pid_t pids[], flight_t flights[],
                        const sim_params_t *params, int pos_fd[2],
                        int report_fd[2]){
    int i;

    for(i=0; i<params->n_flights; i++){
        pids[i] = fork();
        if(pids[i] == -1){
            perror("fork flight");
            return -1;
        }

        if(pids[i] == 0){
            close(pos_fd[0]);
            close(report_fd[0]);
            close(report_fd[1]);
            flight_process(&flights[i], pos_fd[1], params->total_steps);
            close(pos_fd[1]);
            _exit(FLIGHT_EXIT_COMPLETED);
        }
    }

    return 0;
}

int main(int argc, char *argv[]){
    pid_t report_pid;
    pid_t pids[MAX_FLIGHTS];
    int pos_fd[2];
    int report_fd[2];
    int status_raw;
    int i;
    const char *scenario;
    sim_params_t params;
    flight_t flights[MAX_FLIGHTS];
    sim_result_t result;
    sigset_t tick_mask;
    sigset_t old_mask;

    scenario = argc > 1 ? argv[1] : "clean";

    if(load_config_from_json(scenario, &params, flights) != 0)
        return 1;

    if(validate_config(&params, flights) != 0)
        return 1;

    if(pipe(pos_fd) == -1){
        perror("pipe positions");
        return 1;
    }
    if(pipe(report_fd) == -1){
        perror("pipe report");
        close(pos_fd[0]);
        close(pos_fd[1]);
        return 1;
    }

    report_pid = fork_report_process(report_fd, pos_fd);
    if(report_pid == -1){
        close(pos_fd[0]);
        close(pos_fd[1]);
        close(report_fd[0]);
        close(report_fd[1]);
        return 1;
    }

    close(report_fd[0]);
    send_metadata(report_fd[1], &params);

    sigemptyset(&tick_mask);
    sigaddset(&tick_mask, SIGUSR2);
    if(sigprocmask(SIG_BLOCK, &tick_mask, &old_mask) == -1){
        perror("sigprocmask");
        close(pos_fd[0]);
        close(pos_fd[1]);
        close(report_fd[1]);
        return 1;
    }

    if(fork_flights(pids, flights, &params, pos_fd, report_fd) != 0){
        close(pos_fd[0]);
        close(pos_fd[1]);
        close(report_fd[1]);
        return 1;
    }

    if(sigprocmask(SIG_SETMASK, &old_mask, NULL) == -1)
        perror("sigprocmask restore");

    close(pos_fd[1]);

    result = main_loop(pids, &params, pos_fd[0], report_fd[1]);
    close(pos_fd[0]);

    if(result.end_reason == SIM_END_POSITION_READ_ERROR){
        for(i=0; i<params.n_flights; i++)
            kill(pids[i], SIGTERM);
    }

    send_sim_end(report_fd[1], &result);
    send_flight_statuses(report_fd[1], pids, flights, params.n_flights,
                         result.end_reason);

    close(report_fd[1]);

    if(waitpid(report_pid, &status_raw, 0) == -1)
        perror("waitpid report");

    return result.end_reason == SIM_END_POSITION_READ_ERROR ? 1 : 0;
}
