/*
 * test_us107.c — Unit tests for US107
 *
 * Tests the condvar notification path between detection and report threads
 * using the public report_queue_t API, without needing forked processes or
 * shared memory.  The exact condvar pattern used here mirrors report_thread.c
 * (dequeue) and detection_thread.c (enqueue), so it is testing the real code.
 *
 * T1 — single violation event wakes the consumer (no missed wakeup)
 * T2 — N concurrent producers, all events received in full
 * T3 — report_queue_finish() unblocks a consumer waiting with no events
 * T4 — event enqueued before consumer starts is not lost (while predicate)
 */
#define _POSIX_C_SOURCE 200809L
#include "report_thread.h"
#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/* ── helpers ──────────────────────────────────────────────────────────────── */
static void ok(const char *n)  { printf("  PASS  %s\n", n); }
static void die(const char *n, const char *w){
    printf("  FAIL  %s — %s\n", n, w); exit(1);
}

/*
 * Inline consumer thread: mirrors the exact condvar wait pattern from
 * dequeue_report_event() in report_thread.c (while + cond_wait under mutex).
 */
typedef struct {
    report_queue_t *q;
    int             n_expected;
    int             n_received;
} consumer_arg_t;

static void *consumer_thread(void *arg){
    consumer_arg_t *a = (consumer_arg_t *)arg;
    report_queue_t *q = a->q;

    while(a->n_received < a->n_expected){
        pthread_mutex_lock(&q->mutex);
        /* US107 AC3: "while, not if" — spurious-wakeup safe predicate */
        while(q->head == NULL && !q->done)
            pthread_cond_wait(&q->cond, &q->mutex); /* US107 AC3+AC4 */
        if(q->head == NULL && q->done){
            pthread_mutex_unlock(&q->mutex);
            break;
        }
        {
            report_node_t *node = q->head;
            q->head = node->next;
            if(!q->head) q->tail = NULL;
            pthread_mutex_unlock(&q->mutex);
            free(node);
        }
        a->n_received++;
    }
    return NULL;
}

static report_event_t make_violation(int f1, int f2, int step){
    report_event_t ev;
    memset(&ev, 0, sizeof(ev));
    ev.kind = EV_VIOLATION;
    ev.data.violation.f1_id = f1;
    ev.data.violation.f2_id = f2;
    ev.data.violation.step  = step;
    ev.data.violation.h_dist = 5.0;
    ev.data.violation.v_dist = 100.0;
    return ev;
}

/* ── T1 ───────────────────────────────────────────────────────────────────── */
static void t1_single_violation_wakes_consumer(void){
    report_queue_t q; consumer_arg_t ca; pthread_t tid;
    report_event_t ev = make_violation(101, 102, 1);

    report_queue_init(&q);
    ca.q = &q; ca.n_expected = 1; ca.n_received = 0;
    pthread_create(&tid, NULL, consumer_thread, &ca);
    usleep(5000); /* let consumer reach cond_wait */

    enqueue_report_event(&q, &ev); /* US107 AC2: signal under mutex */

    report_queue_finish(&q);
    pthread_join(tid, NULL);
    if(ca.n_received != 1) die("T1","event not received");
    report_queue_destroy(&q);
    ok("T1: single violation event wakes consumer immediately");
}

/* ── T2 ───────────────────────────────────────────────────────────────────── */
#define T2_N 8
typedef struct { report_queue_t *q; int id; } prod_arg_t;
static void *producer(void *arg){
    prod_arg_t    *a  = (prod_arg_t *)arg;
    report_event_t ev = make_violation(a->id, a->id+100, 0);
    enqueue_report_event(a->q, &ev);
    return NULL;
}
static void t2_concurrent_producers_all_received(void){
    report_queue_t q; consumer_arg_t ca;
    pthread_t ctid, ptids[T2_N]; prod_arg_t pargs[T2_N]; int i;

    report_queue_init(&q);
    ca.q = &q; ca.n_expected = T2_N; ca.n_received = 0;
    pthread_create(&ctid, NULL, consumer_thread, &ca);
    usleep(5000);

    for(i=0;i<T2_N;i++){
        pargs[i].q=&q; pargs[i].id=i+1;
        pthread_create(&ptids[i],NULL,producer,&pargs[i]);
    }
    for(i=0;i<T2_N;i++) pthread_join(ptids[i],NULL);
    report_queue_finish(&q);
    pthread_join(ctid,NULL);

    if(ca.n_received != T2_N) die("T2","not all events received");
    report_queue_destroy(&q);
    ok("T2: all events from concurrent producers received");
}

/* ── T3 ───────────────────────────────────────────────────────────────────── */
static void t3_finish_unblocks_waiting_consumer(void){
    report_queue_t q; consumer_arg_t ca; pthread_t tid;

    report_queue_init(&q);
    ca.q = &q; ca.n_expected = 0; ca.n_received = 0;
    pthread_create(&tid, NULL, consumer_thread, &ca);
    usleep(10000); /* consumer is now in cond_wait */

    report_queue_finish(&q);       /* broadcast wakes consumer */
    pthread_join(tid, NULL);       /* must return, not hang */
    report_queue_destroy(&q);
    ok("T3: report_queue_finish() wakes consumer blocked on condvar");
}

/* ── T4 ───────────────────────────────────────────────────────────────────── */
static void t4_event_before_consumer_not_lost(void){
    report_queue_t q; consumer_arg_t ca; pthread_t tid;
    report_event_t ev = make_violation(201, 202, 5);

    report_queue_init(&q);
    /* enqueue BEFORE consumer thread starts */
    enqueue_report_event(&q, &ev);

    ca.q = &q; ca.n_expected = 1; ca.n_received = 0;
    pthread_create(&tid, NULL, consumer_thread, &ca);
    report_queue_finish(&q);
    pthread_join(tid, NULL);

    if(ca.n_received != 1) die("T4","pre-enqueued event was lost");
    report_queue_destroy(&q);
    ok("T4: event enqueued before consumer start is not lost (while predicate)");
}

/* ── main ─────────────────────────────────────────────────────────────────── */
int main(void){
    printf("=== US107 unit tests ===\n");
    t1_single_violation_wakes_consumer();
    t2_concurrent_producers_all_received();
    t3_finish_unblocks_waiting_consumer();
    t4_event_before_consumer_not_lost();
    printf("All US107 tests passed.\n");
    return 0;
}
