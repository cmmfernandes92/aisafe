/*
 * test_us109.c — Unit tests for US109 (generate and store the final report)
 *
 * Drives the report worker (report_generation_thread in report_thread.c):
 * crafts report events, pushes them through the public report_queue_t,
 * lets the thread aggregate and write report.txt, then asserts on the file
 *
 */
#define _POSIX_C_SOURCE 200809L
#include "report_thread.h"
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define REPORT_PATH "test_us109_report.txt"
#define VIOL_PATH   "test_us109_violations.log"

static void ok(const char *n)  { printf("  PASS  %s\n", n); }
static void die(const char *n, const char *w){
    printf("  FAIL  %s — %s\n", n, w); exit(1);
}

static void must_contain(const char *hay, const char *needle, const char *test){
    if(strstr(hay, needle) == NULL) die(test, needle);
}
static void must_not_contain(const char *hay, const char *needle, const char *test){
    if(strstr(hay, needle) != NULL) die(test, needle);
}

static int read_file(const char *path, char *buf, size_t cap){
    FILE *f = fopen(path, "r");
    size_t n;
    if(f == NULL) return -1;
    n = fread(buf, 1, cap - 1, f);
    buf[n] = '\0';
    fclose(f);
    return (int)n;
}

/*  event builders  */
static report_event_t make_meta(const char *scenario, int steps, int n, int maxv){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_METADATA;
    ev.data.meta.total_steps = steps;
    ev.data.meta.n_flights = n;
    ev.data.meta.max_violations = maxv;
    ev.data.meta.h_min = 8.0;
    ev.data.meta.v_min = 600.0;
    ev.data.meta.seconds_per_step = 60.0;
    strncpy(ev.data.meta.scenario, scenario, SCENARIO_NAME_SIZE - 1);
    strncpy(ev.data.meta.run_timestamp, "2026-06-14T10:00:00", RUN_TIMESTAMP_SIZE - 1);
    return ev;
}

static report_event_t make_status(int fid, flight_outcome_t outcome, int code){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_FLIGHT_STATUS;
    ev.data.status.flight_id = fid;
    ev.data.status.outcome = outcome;
    ev.data.status.exit_code = code;
    return ev;
}

/* a separation violation carries a position + velocity vector for both flights */
static report_event_t make_sep(int step, int f1, int f2){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_VIOLATION;
    ev.data.violation.viol_type = VIOL_SEPARATION;
    ev.data.violation.step = step;
    ev.data.violation.f1_id = f1;
    ev.data.violation.f2_id = f2;
    ev.data.violation.h_dist = 3.0;
    ev.data.violation.v_dist = 100.0;
    ev.data.violation.x1 = 11.0; ev.data.violation.y1 = 22.0; ev.data.violation.alt1 = 9000.0;
    ev.data.violation.vx1 = 33.0; ev.data.violation.vy1 = 44.0; ev.data.violation.vz1 = 55.0;
    ev.data.violation.x2 = 66.0; ev.data.violation.y2 = 77.0; ev.data.violation.alt2 = 9050.0;
    ev.data.violation.vx2 = 88.0; ev.data.violation.vy2 = 99.0; ev.data.violation.vz2 = 12.0;
    return ev;
}

/* a single-flight altitude violation: same flight twice, h_dist 0 */
static report_event_t make_alt(int step, int fid, int viol_type, double dist_past){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_VIOLATION;
    ev.data.violation.viol_type = viol_type;
    ev.data.violation.step = step;
    ev.data.violation.f1_id = fid;
    ev.data.violation.f2_id = fid;
    ev.data.violation.h_dist = 0.0;
    ev.data.violation.v_dist = dist_past;
    ev.data.violation.alt1 = 8349.0;
    ev.data.violation.alt2 = 8349.0;
    return ev;
}

static report_event_t make_end(sim_end_reason_t reason, int steps, int vcount){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_SIM_END;
    ev.data.end.reason = reason;
    ev.data.end.completed_steps = steps;
    ev.data.end.violation_count = vcount;
    return ev;
}

/* Push events through the real report thread and read back the report file. */
static void run_and_read(report_event_t *evs, int n, char *out, size_t cap){
    report_queue_t q;
    report_thread_arg_t arg;
    pthread_t tid;
    int i;

    remove(REPORT_PATH);
    remove(VIOL_PATH);
    report_queue_init(&q);
    arg.queue = &q;
    arg.report_path = REPORT_PATH;
    arg.violations_path = VIOL_PATH;
    if(pthread_create(&tid, NULL, report_generation_thread, &arg) != 0)
        die("setup", "pthread_create");
    for(i = 0; i < n; i++)
        enqueue_report_event(&q, &evs[i]);
    report_queue_finish(&q);
    pthread_join(tid, NULL);
    report_queue_destroy(&q);
    if(read_file(REPORT_PATH, out, cap) < 0)
        die("setup", "report file not written");
}

/* successful run -> PASS, no violations  */
static void t1_success(void){
    report_event_t evs[] = {
        make_meta("clean", 20, 2, 3),
        make_status(101, FLIGHT_COMPLETED, 0),
        make_status(102, FLIGHT_COMPLETED, 0),
        make_end(SIM_END_NORMAL, 20, 0),
    };
    char r[8192];
    run_and_read(evs, 4, r, sizeof(r));

    must_contain(r, "scenario=clean", "T1");
    must_contain(r, "total_flights=2", "T1");
    must_contain(r, "end_reason=normal", "T1");
    must_contain(r, "stored_violation_events=0", "T1");
    must_contain(r, "validation=PASS", "T1");
    /* structure: both sections and the kind column header are present */
    must_contain(r, "[flight_statuses]", "T1");
    must_contain(r, "[violations]", "T1");
    must_contain(r, "vx1,vy1,vz1", "T1");
    must_contain(r, ",kind", "T1");
    ok("T1: successful run -> validation=PASS, 0 violations");
}

/* T2: threshold exceeded -> FAIL, separation violation logged  */
static void t2_failed_threshold(void){
    report_event_t evs[] = {
        make_meta("threshold", 20, 2, 0),
        make_sep(9, 301, 302),
        make_status(301, FLIGHT_TERMINATED_DUE_TO_VIOLATION, 2),
        make_status(302, FLIGHT_TERMINATED_DUE_TO_VIOLATION, 2),
        make_end(SIM_END_THRESHOLD_EXCEEDED, 10, 1),
    };
    char r[8192];
    run_and_read(evs, 5, r, sizeof(r));

    must_contain(r, "end_reason=threshold_exceeded", "T2");
    must_contain(r, "stored_violation_events=1", "T2");
    must_contain(r, "validation=FAIL", "T2");
    /* US109.3: the violation row carries step (timestamp basis) + velocity vectors */
    must_contain(r, "9,301,302", "T2");
    must_contain(r, "33.00", "T2");      /* vx1 from make_sep */
    must_contain(r, "55.00", "T2");      /* vz1 from make_sep */
    must_contain(r, "separation", "T2");
    ok("T2: threshold exceeded -> validation=FAIL, separation row");
}

/* T3: mixed outcomes + altitude violation -> FAIL (not all completed) */
static void t3_mixed_outcomes(void){
    report_event_t evs[] = {
        make_meta("mixed", 20, 3, 5),
        make_sep(7, 201, 202),
        make_alt(14, 203, VIOL_ALT_CEILING, 349.0),
        make_status(201, FLIGHT_COMPLETED, 0),
        make_status(202, FLIGHT_TERMINATED_DUE_TO_VIOLATION, 2),
        make_status(203, FLIGHT_CRASHED, 139),
        make_end(SIM_END_NORMAL, 20, 2),
    };
    char r[8192];
    run_and_read(evs, 7, r, sizeof(r));

    /* every outcome name is rendered */
    must_contain(r, "completed", "T3");
    must_contain(r, "terminated_due_to_violation", "T3");
    must_contain(r, "crashed", "T3");
    /* both violation kinds present, tagged in the kind column */
    must_contain(r, "separation", "T3");
    must_contain(r, "altitude_ceiling", "T3");
    /* altitude row is single-flight: same id twice */
    must_contain(r, "14,203,203", "T3");
    /* verdict: ended normally and within threshold, but not all flights
       completed -> FAIL (exercises the all-completed rule) */
    must_contain(r, "stored_violation_events=2", "T3");
    must_contain(r, "validation=FAIL", "T3");
    ok("T3: mixed outcomes + altitude violation -> kinds tagged, FAIL");
}

/* T4: violations within the allowed maximum, all complete -> PASS */
static void t4_within_threshold(void){
    report_event_t evs[] = {
        make_meta("violation", 20, 2, 5),
        make_sep(9, 201, 202),
        make_status(201, FLIGHT_COMPLETED, 0),
        make_status(202, FLIGHT_COMPLETED, 0),
        make_end(SIM_END_NORMAL, 20, 1),
    };
    char r[8192];
    run_and_read(evs, 5, r, sizeof(r));

    must_contain(r, "stored_violation_events=1", "T4");
    must_contain(r, "validation=PASS", "T4");   /* 1 <= max 5, all completed */
    must_not_contain(r, "validation=FAIL", "T4");
    ok("T4: violations within max + all completed -> validation=PASS");
}

int main(void){
    printf("=== US109 unit tests ===\n");
    t1_success();
    t2_failed_threshold();
    t3_mixed_outcomes();
    t4_within_threshold();
    remove(REPORT_PATH);
    remove(VIOL_PATH);
    printf("All US109 tests passed.\n");
    return 0;
}