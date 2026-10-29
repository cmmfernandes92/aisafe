/*
 * test_scenarios.c — Hardcoded scenario tests for the flight simulation
 *
 * Runs ./flight_sim with each JSON scenario as a child process, then
 * verifies the exit code and the content of violations.log:
 *
 *   clean.json      — 1 flight, no expected violations  → log empty
 *   violation.json  — 3 flights, violations expected,
 *                     max_violations=10 (not exceeded)  → log non-empty, exit 0
 *   threshold.json  — 3 flights, max_violations=0
 *                     (first violation stops sim)       → log non-empty, exit 0
 *
 * Note: all three scenarios return exit 0 (only SIM_END_POSITION_READ_ERROR
 * returns exit 1, per flight_sim.c main()).
 *
 * Compile: gcc -Wall -Wextra -g -o test_scenarios test_scenarios.c
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

/* ── Test runner ── */
static int tests_run    = 0;
static int tests_passed = 0;

#define RUN(fn) do { \
    tests_run++; \
    printf("  %-55s", #fn); \
    fflush(stdout); \
    fn(); \
    tests_passed++; \
    printf("PASS\n"); \
} while(0)

/* ── Helpers ── */

/* Run ./flight_sim <scenario> and return its exit code.
   stderr is suppressed (the sim prints diagnostics there). */
static int run_sim(const char *scenario){
    pid_t pid = fork();
    assert(pid != -1);

    if(pid == 0){
        /* redirect stderr to /dev/null — suppress diagnostic prints */
        int devnull = open("/dev/null", O_WRONLY);
        if(devnull != -1){
            dup2(devnull, STDERR_FILENO);
            close(devnull);
        }
        /* also suppress stdout from the simulation itself */
        int outnull = open("/dev/null", O_WRONLY);
        if(outnull != -1){
            dup2(outnull, STDOUT_FILENO);
            close(outnull);
        }
        char *args[] = { "./flight_sim", (char *)scenario, NULL };
        execv("./flight_sim", args);
        _exit(127); /* exec failed */
    }

    int status;
    waitpid(pid, &status, 0);
    if(WIFEXITED(status))
        return WEXITSTATUS(status);
    return -1;
}

/* Returns 1 if violations.log exists and has content, 0 otherwise */
static int log_has_violations(void){
    struct stat st;
    if(stat("violations.log", &st) != 0)
        return 0;
    return st.st_size > 0;
}

static void remove_artifacts(void){
    remove("violations.log");
    remove("report.txt");
}

/* ════════════════════════════════════════════
 * SCENARIO: clean.json
 * 1 flight, well-separated route, max_violations=3
 * ════════════════════════════════════════════ */

/* Simulation must exit cleanly (no read error) */
static void test_clean_exits_zero(void){
    remove_artifacts();
    int rc = run_sim("clean.json");
    assert(rc == 0);
    remove_artifacts();
}

/* No flights violate the safety distance → log must be empty */
static void test_clean_no_violations_in_log(void){
    remove_artifacts();
    run_sim("clean.json");
    assert(log_has_violations() == 0);
    remove_artifacts();
}

/* report.txt is produced after a successful run */
static void test_clean_report_created(void){
    remove_artifacts();
    run_sim("clean.json");
    struct stat st;
    assert(stat("report.txt", &st) == 0);
    assert(st.st_size > 0);
    remove_artifacts();
}

/* ════════════════════════════════════════════
 * SCENARIO: violation.json
 * 3 flights, pair that violates, max_violations=10 (not exceeded)
 * ════════════════════════════════════════════ */

/* Simulation must exit cleanly even with violations (threshold not hit) */
static void test_violation_exits_zero(void){
    remove_artifacts();
    int rc = run_sim("violation.json");
    assert(rc == 0);
    remove_artifacts();
}

/* At least one violation must appear in violations.log */
static void test_violation_log_is_non_empty(void){
    remove_artifacts();
    run_sim("violation.json");
    assert(log_has_violations() == 1);
    remove_artifacts();
}

/* violations.log must contain the "step=" key written by check_violations() */
static void test_violation_log_contains_step_field(void){
    remove_artifacts();
    run_sim("violation.json");

    FILE *f = fopen("violations.log", "r");
    assert(f != NULL);
    char line[256];
    int found = 0;
    while(fgets(line, sizeof(line), f)){
        if(strstr(line, "step=") != NULL){ found = 1; break; }
    }
    fclose(f);
    assert(found == 1);
    remove_artifacts();
}

/* report.txt must be produced */
static void test_violation_report_created(void){
    remove_artifacts();
    run_sim("violation.json");
    struct stat st;
    assert(stat("report.txt", &st) == 0);
    assert(st.st_size > 0);
    remove_artifacts();
}

/* ════════════════════════════════════════════
 * SCENARIO: threshold.json
 * 3 flights, max_violations=0 — first violation stops the simulation
 * ════════════════════════════════════════════ */

/* Even with early termination, exit code must be 0 (not a read error) */
static void test_threshold_exits_zero(void){
    remove_artifacts();
    int rc = run_sim("threshold.json");
    assert(rc == 0);
    remove_artifacts();
}

/* The violation that triggered termination must appear in violations.log */
static void test_threshold_log_has_at_least_one_violation(void){
    remove_artifacts();
    run_sim("threshold.json");
    assert(log_has_violations() == 1);
    remove_artifacts();
}

/* report.txt must still be produced even when threshold is exceeded */
static void test_threshold_report_created(void){
    remove_artifacts();
    run_sim("threshold.json");
    struct stat st;
    assert(stat("report.txt", &st) == 0);
    assert(st.st_size > 0);
    remove_artifacts();
}

/* ════════════════════════════════════════════
 * MAIN
 * ════════════════════════════════════════════ */
int main(void){
    printf("=== Scenario Tests (hardcoded) ===\n\n");

    printf("clean.json (1 flight, no violations expected):\n");
    RUN(test_clean_exits_zero);
    RUN(test_clean_no_violations_in_log);
    RUN(test_clean_report_created);

    printf("\nviolation.json (3 flights, violations under threshold):\n");
    RUN(test_violation_exits_zero);
    RUN(test_violation_log_is_non_empty);
    RUN(test_violation_log_contains_step_field);
    RUN(test_violation_report_created);

    printf("\nthreshold.json (3 flights, first violation stops sim):\n");
    RUN(test_threshold_exits_zero);
    RUN(test_threshold_log_has_at_least_one_violation);
    RUN(test_threshold_report_created);

    printf("\n=== %d/%d tests passed ===\n", tests_passed, tests_run);
    return (tests_passed == tests_run) ? 0 : 1;
}