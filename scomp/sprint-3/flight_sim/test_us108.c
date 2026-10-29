/*
 * test_us108.c — Unit tests for US108: semaphore-based per-step lockstep.
 *
 * Tests use per-flight ticks / sem_done directly (no forked flight processes)
 * to verify the parent-child step protocol is correct in isolation. Like
 * in the sim, each flight has its own tick so a post wakes exactly that flight:
 *
 *   T1 — with N=1 flight, parent and flight advance exactly in lockstep
 *         (parent never opens step k+1 before flight posts done for step k).
 *   T2 — with N=5 flights, all 5 must complete step k before step k+1 opens.
 *   T3 — ordering guarantee: parent collects all done tokens before advancing.
 *   T4 — early termination: setting simulation_running=0 + posting drain ticks
 *         unblocks a flight waiting on its tick without deadlock.
 */

#define _POSIX_C_SOURCE 200809L
#include "types.h"
#include <assert.h>
#include <fcntl.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

/* test semaphore names (different from production names to avoid conflicts).
 * one tick per flight, like production: tick[i] only wakes flight i. */
#define TEST_SEM_TICK_PREFIX "/test_us108_tick_"
#define TEST_SEM_DONE  "/test_us108_done"
#define TEST_SEM_MX    "/test_us108_mx"
#define TEST_STEPS     6
#define TEST_MAX_N     10

static void pass(const char *n){ printf("  PASS  %s\n", n); }
static void fail(const char *n, const char *w){ printf("  FAIL  %s: %s\n",n,w); exit(1); }

/* open n per-flight tick semaphores (init 0) into tick[] */
static void open_ticks(sem_t *tick[], int n){
    char name[48];
    int  i;
    for(i=0; i<n; i++){
        snprintf(name, sizeof(name), "%s%d", TEST_SEM_TICK_PREFIX, i);
        sem_unlink(name);
        tick[i] = sem_open(name, O_CREAT|O_EXCL, 0644, 0);
        if(tick[i] == SEM_FAILED) fail("setup","sem_open tick");
    }
}

/* close + unlink the n ticks and the done semaphore */
static void sem_cleanup(sem_t *tick[], int n, sem_t *done){
    char name[48];
    int  i;
    for(i=0; i<n; i++){
        sem_close(tick[i]);
        snprintf(name, sizeof(name), "%s%d", TEST_SEM_TICK_PREFIX, i);
        sem_unlink(name);
    }
    sem_close(done); sem_unlink(TEST_SEM_DONE);
}

/* ── simulated "flight thread" ────────────────────────────────────────────── */
typedef struct {
    sem_t  *tick;
    sem_t  *done;
    int     n_steps;
    int    *sim_running;   /* pointer into shared int (simulates shm flag) */
    int     steps_done;    /* filled in by thread on exit */
    int     id;
} flight_arg_t;

static void *flight_thread(void *arg){
    flight_arg_t *a = (flight_arg_t *)arg;
    int step;

    for(step = 0; step < a->n_steps; step++){
        /* mirrors flight_proc.c: wait for tick, check shutdown flag */
        while(sem_wait(a->tick) == -1 && errno == EINTR && *a->sim_running)
            ;
        if(!*a->sim_running)
            break;
        /* simulate some work */
        usleep(100);
        /* post done */
        sem_post(a->done);
    }
    a->steps_done = step;
    return NULL;
}

/* ── T1: N=1 lockstep ─────────────────────────────────────────────────────── */
static void t1_single_flight_lockstep(void){
    sem_t        *tick[1], *done;
    flight_arg_t  fa;
    pthread_t     tid;
    int           sim_running = 1;
    int           step;

    open_ticks(tick, 1);
    sem_unlink(TEST_SEM_DONE);
    done = sem_open(TEST_SEM_DONE, O_CREAT|O_EXCL, 0644, 0);
    if(done==SEM_FAILED) fail("T1","sem_open");

    fa.tick=tick[0]; fa.done=done; fa.n_steps=TEST_STEPS;
    fa.sim_running=&sim_running; fa.steps_done=0; fa.id=1;
    pthread_create(&tid, NULL, flight_thread, &fa);

    for(step=0; step<TEST_STEPS; step++){
        sem_post(tick[0]);           /* open step for flight */
        sem_wait(done);              /* barrier: wait for flight to finish */
    }

    pthread_join(tid, NULL);

    if(fa.steps_done != TEST_STEPS)
        fail("T1","flight did not complete all steps");

    sem_cleanup(tick, 1, done);
    pass("T1: N=1 parent/flight advance in lockstep");
}

/* ── T2: N=5 all flights finish step k before k+1 opens ─────────────────── */
#define T2_N 5

/* shared step counter — checked by flights to verify ordering */
static volatile int t2_current_step;
static volatile int t2_violations;

static void *flight_thread_t2(void *arg){
    flight_arg_t *a = (flight_arg_t *)arg;
    int step;

    for(step=0; step<a->n_steps; step++){
        while(sem_wait(a->tick)==-1 && errno==EINTR && *a->sim_running)
            ;
        if(!*a->sim_running) break;
        /* ordering check: t2_current_step must equal step */
        if(t2_current_step != step)
            t2_violations++;
        usleep(50);
        sem_post(a->done);
    }
    a->steps_done = step;
    return NULL;
}

static void t2_n_flights_all_finish_before_next_step(void){
    sem_t        *tick[T2_N], *done;
    flight_arg_t  fa[T2_N];
    pthread_t     tids[T2_N];
    int           sim_running = 1;
    int           i, step;

    open_ticks(tick, T2_N);
    sem_unlink(TEST_SEM_DONE);
    done = sem_open(TEST_SEM_DONE, O_CREAT|O_EXCL, 0644, 0);
    if(done==SEM_FAILED) fail("T2","sem_open");

    t2_current_step = 0;
    t2_violations   = 0;

    for(i=0; i<T2_N; i++){
        fa[i].tick=tick[i]; fa[i].done=done; fa[i].n_steps=TEST_STEPS;
        fa[i].sim_running=&sim_running; fa[i].steps_done=0; fa[i].id=i;
        pthread_create(&tids[i], NULL, flight_thread_t2, &fa[i]);
    }

    for(step=0; step<TEST_STEPS; step++){
        t2_current_step = step;
        for(i=0; i<T2_N; i++) sem_post(tick[i]); /* release each flight */
        for(i=0; i<T2_N; i++) sem_wait(done);    /* collect all N */
    }

    for(i=0; i<T2_N; i++) pthread_join(tids[i], NULL);

    for(i=0; i<T2_N; i++)
        if(fa[i].steps_done != TEST_STEPS)
            fail("T2","a flight did not complete all steps");

    if(t2_violations > 0)
        fail("T2","step ordering violated — a flight ran on wrong step");

    sem_cleanup(tick, T2_N, done);
    pass("T2: N=5 all flights finish step k before parent opens step k+1");
}

/* ── T3: done token count matches exactly ────────────────────────────────── */
static void t3_done_count_exact(void){
    /* With N flights and S steps the total number of done tokens posted must
     * equal N*S. Per-flight ticks make this exact: no flight can grab anothers
     * tick and double-step, so done never ends with leftover tokens. */
    sem_t        *tick[3], *done;
    flight_arg_t  fa[3];
    pthread_t     tids[3];
    int           sim_running = 1;
    int           i, step;
    const int     N=3, S=TEST_STEPS;

    open_ticks(tick, N);
    sem_unlink(TEST_SEM_DONE);
    done = sem_open(TEST_SEM_DONE, O_CREAT|O_EXCL, 0644, 0);
    if(done==SEM_FAILED) fail("T3","sem_open");

    for(i=0; i<N; i++){
        fa[i].tick=tick[i]; fa[i].done=done; fa[i].n_steps=S;
        fa[i].sim_running=&sim_running; fa[i].steps_done=0; fa[i].id=i;
        pthread_create(&tids[i], NULL, flight_thread, &fa[i]);
    }

    int total_collected = 0;
    for(step=0; step<S; step++){
        for(i=0; i<N; i++) sem_post(tick[i]);
        for(i=0; i<N; i++){ sem_wait(done); total_collected++; }
    }

    for(i=0; i<N; i++) pthread_join(tids[i], NULL);

    if(total_collected != N*S)
        fail("T3","done token count mismatch");

    /* semaphore value must be 0 now — nothing left over */
    int val = -1;
    sem_getvalue(done, &val);
    if(val != 0)
        fail("T3","sem_done has leftover tokens after full run");

    sem_cleanup(tick, N, done);
    pass("T3: done token count is exactly N*S with zero leftover");
}

/* ── T4: shutdown via sim_running=0 + drain tick unblocks flight ─────────── */
static void t4_early_termination_no_deadlock(void){
    sem_t        *tick[1], *done;
    flight_arg_t  fa;
    pthread_t     tid;
    int           sim_running = 1;

    open_ticks(tick, 1);
    sem_unlink(TEST_SEM_DONE);
    done = sem_open(TEST_SEM_DONE, O_CREAT|O_EXCL, 0644, 0);
    if(done==SEM_FAILED) fail("T4","sem_open");

    fa.tick=tick[0]; fa.done=done; fa.n_steps=100; /* many steps */
    fa.sim_running=&sim_running; fa.steps_done=0; fa.id=1;
    pthread_create(&tid, NULL, flight_thread, &fa);

    /* run 2 normal steps */
    int step;
    for(step=0; step<2; step++){
        sem_post(tick[0]);
        sem_wait(done);
    }

    /* simulate early termination: clear flag, post one drain tick */
    sim_running = 0;
    sem_post(tick[0]); /* drain token — wakes the flight, it sees !sim_running */

    /* must join without hanging */
    pthread_join(tid, NULL);

    if(fa.steps_done > 3)   /* at most one extra step possible */
        fail("T4","flight ran too many steps after shutdown");

    sem_cleanup(tick, 1, done);
    pass("T4: shutdown via sim_running=0 + drain tick — no deadlock");
}

/* ── main ─────────────────────────────────────────────────────────────────── */
int main(void){
    /* clean up any stale semaphores from previous runs (ticks are cleared
     * per-index inside open_ticks) */
    sem_unlink(TEST_SEM_DONE);
    sem_unlink(TEST_SEM_MX);

    printf("=== US108 unit tests ===\n");
    t1_single_flight_lockstep();
    t2_n_flights_all_finish_before_next_step();
    t3_done_count_exact();
    t4_early_termination_no_deadlock();
    printf("All US108 tests passed.\n");
    return 0;
}
