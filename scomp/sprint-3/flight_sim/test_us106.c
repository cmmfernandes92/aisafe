/*
 * test_us106.c — Validation tests for US106 function-specific parent threads
 *
 * These tests combine static checks (to prove that the requested pthread/condvar
 * mechanisms exist in the implementation) with functional scenario checks
 * (to prove that the threaded detection/report flow still produces the expected
 * simulation artifacts).
 */

#define _POSIX_C_SOURCE 200809L

#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

static int tests_run = 0;
static int tests_passed = 0;

#define RUN(fn) do { \
    tests_run++; \
    printf("  %-70s", #fn); \
    fflush(stdout); \
    fn(); \
    tests_passed++; \
    printf("PASS\n"); \
} while(0)

static char *read_text_file(const char *path){
    FILE *f;
    long size;
    char *buffer;

    f = fopen(path, "rb");
    assert(f != NULL);

    assert(fseek(f, 0, SEEK_END) == 0);
    size = ftell(f);
    assert(size >= 0);
    rewind(f);

    buffer = malloc((size_t)size + 1);
    assert(buffer != NULL);

    assert(fread(buffer, 1, (size_t)size, f) == (size_t)size);
    buffer[size] = '\0';
    fclose(f);
    return buffer;
}

static int file_contains(const char *path, const char *needle){
    char *content = read_text_file(path);
    int found = strstr(content, needle) != NULL;
    free(content);
    return found;
}

static int file_not_contains(const char *path, const char *needle){
    return !file_contains(path, needle);
}

static void remove_artifacts(void){
    remove("violations.log");
    remove("report.txt");
}

static int run_sim(const char *scenario){
    pid_t pid = fork();
    assert(pid != -1);

    if(pid == 0){
        int devnull = open("/dev/null", O_WRONLY);
        if(devnull != -1){
            dup2(devnull, STDERR_FILENO);
            dup2(devnull, STDOUT_FILENO);
            close(devnull);
        }

        char *args[] = { "./flight_sim", (char *)scenario, NULL };
        execv("./flight_sim", args);
        _exit(127);
    }

    int status;
    assert(waitpid(pid, &status, 0) == pid);
    if(WIFEXITED(status))
        return WEXITSTATUS(status);
    return -1;
}

static void assert_report_contains(const char *needle){
    assert(file_contains("report.txt", needle));
}

static void test_parent_creates_detection_and_report_threads(void){
    assert(file_contains("flight_sim.c", "pthread_create(&t_detection"));
    assert(file_contains("flight_sim.c", "violation_detection_thread"));
    assert(file_contains("flight_sim.c", "pthread_create(&t_report"));
    assert(file_contains("flight_sim.c", "report_generation_thread"));
}

static void test_detection_thread_waits_for_step_with_condvar(void){
    assert(file_contains("detection_thread.c", "pthread_cond_wait(&ctx->step_ready_cond"));
    assert(file_contains("detection_thread.c", "while(!ctx->step_ready && !ctx->simulation_done)"));
    assert(file_contains("flight_sim.c", "pthread_cond_signal(&det_ctx->step_ready_cond"));
}

static void test_report_thread_waits_for_events_with_condvar(void){
    assert(file_contains("report_thread.c", "pthread_cond_wait(&queue->cond"));
    assert(file_contains("report_thread.c", "while(queue->head == NULL && !queue->done)"));
    assert(file_contains("report_thread.c", "pthread_cond_signal(&queue->cond"));
    assert(file_contains("report_thread.c", "pthread_cond_broadcast(&queue->cond"));
}

static void test_detection_thread_scans_shared_memory_under_semaphore(void){
    assert(file_contains("detection_thread.c", "sem_wait(ctx->sem_mutex)"));
    assert(file_contains("detection_thread.c", "ctx->shm->flights"));
    assert(file_contains("detection_thread.c", "sem_post(ctx->sem_mutex)"));
}

static void test_main_loop_no_longer_calls_check_violations_directly(void){
    assert(file_not_contains("flight_sim.c", "check_violations("));
}

static void test_report_process_is_not_started_by_parent(void){
    assert(file_not_contains("flight_sim.c", "fork_report_process"));
    assert(file_not_contains("flight_sim.c", "report_process("));
}

static void test_clean_scenario_report_has_no_violations(void){
    remove_artifacts();
    assert(run_sim("clean.json") == 0);
    assert_report_contains("scenario=clean");
    assert_report_contains("reported_violation_count=0");
    assert_report_contains("stored_violation_events=0");
    assert_report_contains("validation=PASS");
    remove_artifacts();
}

static void test_violation_scenario_report_has_violation_event(void){
    remove_artifacts();
    assert(run_sim("violation.json") == 0);
    assert_report_contains("scenario=violation");
    assert_report_contains("reported_violation_count=1");
    assert_report_contains("stored_violation_events=1");
    assert_report_contains("validation=PASS");
    assert_report_contains("vx1");
    remove_artifacts();
}

static void test_threshold_scenario_report_records_threshold_end(void){
    remove_artifacts();
    assert(run_sim("threshold.json") == 0);
    assert_report_contains("scenario=threshold");
    assert_report_contains("end_reason=threshold_exceeded");
    assert_report_contains("validation=FAIL");
    assert_report_contains("reported_violation_count=1");
    remove_artifacts();
}

int main(void){
    printf("=== US106 Tests (function-specific parent threads) ===\n\n");

    RUN(test_parent_creates_detection_and_report_threads);
    RUN(test_detection_thread_waits_for_step_with_condvar);
    RUN(test_report_thread_waits_for_events_with_condvar);
    RUN(test_detection_thread_scans_shared_memory_under_semaphore);
    RUN(test_main_loop_no_longer_calls_check_violations_directly);
    RUN(test_report_process_is_not_started_by_parent);
    RUN(test_clean_scenario_report_has_no_violations);
    RUN(test_violation_scenario_report_has_violation_event);
    RUN(test_threshold_scenario_report_records_threshold_end);

    printf("\n=== %d/%d tests passed ===\n", tests_passed, tests_run);
    return (tests_passed == tests_run) ? 0 : 1;
}
