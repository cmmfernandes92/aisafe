/*
 * test_us105.c — Unit tests for US105
 *
 * Tests each POSIX primitive used by US105 in isolation:
 * shared memory lifecycle, semaphore values/operations,
 * thread create/join, mutex + condition variable, barrier pattern.
 *
 * No external framework — uses assert() from <assert.h>.
 * Compile: gcc -Wall -Wextra -g -o test_us105 test_us105.c -pthread [-lrt]
 */

#define _POSIX_C_SOURCE 200809L

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#include "types.h"

/* Distinct names — do not collide with a running simulation */
#define T_SHM     "/test_aisafe_shm"
#define T_MUTEX   "/test_aisafe_mutex"
#define T_BARRIER "/test_aisafe_barrier"
#define T_NPROC   "/test_aisafe_nproc"

/* ── Test runner ── */
static int tests_run    = 0;
static int tests_passed = 0;

#define RUN(fn) do { \
    tests_run++; \
    printf("  %-52s", #fn); \
    fflush(stdout); \
    fn(); \
    tests_passed++; \
    printf("PASS\n"); \
} while(0)

/* ── Helpers ── */
static void cleanup_all(void){
    shm_unlink(T_SHM);
    sem_unlink(T_MUTEX);
    sem_unlink(T_BARRIER);
    sem_unlink(T_NPROC);
}

static SharedData *open_shm(int *fd_out){
    int fd = shm_open(T_SHM, O_CREAT | O_EXCL | O_RDWR, S_IRUSR | S_IWUSR);
    assert(fd != -1);
    assert(ftruncate(fd, sizeof(SharedData)) == 0);
    SharedData *shm = mmap(NULL, sizeof(SharedData),
                           PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    assert(shm != MAP_FAILED);
    memset(shm, 0, sizeof(SharedData));
    *fd_out = fd;
    return shm;
}

static void close_shm(SharedData *shm, int fd){
    munmap(shm, sizeof(SharedData));
    close(fd);
    shm_unlink(T_SHM);
}

/* ════════════════════════════════════════════
 * SHARED MEMORY
 * ════════════════════════════════════════════ */

/* shm_open cria o segmento; memset inicializa campos a zero */
static void test_shm_create_and_init(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    shm->total_flights      = 3;
    shm->simulation_running = 1;
    shm->nproc_at_barrier   = 0;

    assert(shm->total_flights      == 3);
    assert(shm->simulation_running == 1);
    assert(shm->nproc_at_barrier   == 0);

    close_shm(shm, fd);
}

/* O_EXCL: segunda shm_open com mesmo nome falha com EEXIST */
static void test_shm_exclusive_create(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    int fd2 = shm_open(T_SHM, O_CREAT | O_EXCL | O_RDWR, S_IRUSR | S_IWUSR);
    assert(fd2 == -1);
    assert(errno == EEXIST);

    close_shm(shm, fd);
}

/* Cada slot flights[i] é acessível por índice e persiste em shm */
static void test_shm_flight_slots_readable(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    shm->total_flights = 3;
    for(int i = 0; i < 3; i++){
        shm->flights[i].flight_id = 100 + i;
        shm->flights[i].step      = i * 5;
        shm->flights[i].x         = (double)(i * 1000);
        shm->flights[i].alt       = 9000.0;
        shm->flights[i].active    = 1;
    }
    for(int i = 0; i < 3; i++){
        assert(shm->flights[i].flight_id == 100 + i);
        assert(shm->flights[i].step      == i * 5);
        assert(shm->flights[i].active    == 1);
    }

    close_shm(shm, fd);
}

/* Após shm_unlink, nova shm_open começa com campos a zero */
static void test_shm_reopen_after_unlink_is_clean(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);
    shm->total_flights = 99;
    close_shm(shm, fd);

    shm = open_shm(&fd);
    assert(shm->total_flights == 0);
    close_shm(shm, fd);
}

/* simulation_running pode ser alterado de 1 para 0 e lido de volta */
static void test_shm_simulation_running_toggle(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    shm->simulation_running = 1;
    assert(shm->simulation_running == 1);
    shm->simulation_running = 0;
    assert(shm->simulation_running == 0);

    close_shm(shm, fd);
}

/* active=0 num slot não afecta os restantes */
static void test_shm_active_flag_independent_per_slot(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    shm->total_flights = 3;
    for(int i = 0; i < 3; i++) shm->flights[i].active = 1;

    shm->flights[1].active = 0;   /* só o slot 1 fica inativo */

    assert(shm->flights[0].active == 1);
    assert(shm->flights[1].active == 0);
    assert(shm->flights[2].active == 1);

    close_shm(shm, fd);
}

/* Todos os MAX_FLIGHTS slots são acessíveis (índice máximo não sai fora) */
static void test_shm_all_max_slots_accessible(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);

    for(int i = 0; i < MAX_FLIGHTS; i++){
        shm->flights[i].flight_id = i + 1;
        shm->flights[i].active    = 1;
    }
    assert(shm->flights[MAX_FLIGHTS - 1].flight_id == MAX_FLIGHTS);
    assert(shm->flights[MAX_FLIGHTS - 1].active    == 1);

    close_shm(shm, fd);
}

/* ════════════════════════════════════════════
 * SEMÁFOROS
 * ════════════════════════════════════════════ */

/* SEM_MUTEX criado com valor inicial 1: trywait tem sucesso uma vez */
static void test_sem_mutex_starts_at_1(void){
    cleanup_all();
    sem_t *sem = sem_open(T_MUTEX, O_CREAT | O_EXCL, 0644, 1);
    assert(sem != SEM_FAILED);

    assert(sem_trywait(sem) == 0);               /* 1 → 0: sucesso */
    assert(sem_trywait(sem) == -1);              /* 0: falha imediatamente */
    assert(errno == EAGAIN);

    sem_post(sem);
    sem_close(sem);
    sem_unlink(T_MUTEX);
}

/* SEM_BARRIER criado com valor inicial 0: trywait falha imediatamente */
static void test_sem_barrier_starts_at_0(void){
    cleanup_all();
    sem_t *sem = sem_open(T_BARRIER, O_CREAT | O_EXCL, 0644, 0);
    assert(sem != SEM_FAILED);

    assert(sem_trywait(sem) == -1);
    assert(errno == EAGAIN);

    sem_close(sem);
    sem_unlink(T_BARRIER);
}

/* sem_post incrementa: depois de post, trywait tem sucesso */
static void test_sem_post_increments(void){
    cleanup_all();
    sem_t *sem = sem_open(T_MUTEX, O_CREAT | O_EXCL, 0644, 0);
    assert(sem != SEM_FAILED);

    assert(sem_trywait(sem) == -1);  /* valor 0 */
    sem_post(sem);                   /* valor 1 */
    assert(sem_trywait(sem) == 0);   /* decrementou para 0 */

    sem_close(sem);
    sem_unlink(T_MUTEX);
}

/* N posts permitem N trywait consecutivos com sucesso */
static void test_sem_multiple_posts_allow_multiple_waits(void){
    cleanup_all();
    sem_t *sem = sem_open(T_MUTEX, O_CREAT | O_EXCL, 0644, 0);
    assert(sem != SEM_FAILED);

    sem_post(sem);
    sem_post(sem);
    sem_post(sem);                       /* valor = 3 */

    assert(sem_trywait(sem) == 0);       /* 3 → 2 */
    assert(sem_trywait(sem) == 0);       /* 2 → 1 */
    assert(sem_trywait(sem) == 0);       /* 1 → 0 */
    assert(sem_trywait(sem) == -1);      /* 0 → falha */

    sem_close(sem);
    sem_unlink(T_MUTEX);
}

/* sem_open com flag=0 abre semáforo já existente sem criar */
static void test_sem_open_existing_without_create(void){
    cleanup_all();
    sem_t *sem1 = sem_open(T_MUTEX, O_CREAT | O_EXCL, 0644, 2);
    assert(sem1 != SEM_FAILED);
    sem_close(sem1);

    /* abre o mesmo semáforo sem O_CREAT — valor continua 2 */
    sem_t *sem2 = sem_open(T_MUTEX, 0);
    assert(sem2 != SEM_FAILED);
    assert(sem_trywait(sem2) == 0);      /* 2 → 1 */
    assert(sem_trywait(sem2) == 0);      /* 1 → 0 */
    assert(sem_trywait(sem2) == -1);     /* 0 → falha */

    sem_close(sem2);
    sem_unlink(T_MUTEX);
}

/* sem_mutex protege secção crítica: escrita na shm é visível após sem_post */
static void test_sem_guards_shm_write(void){
    cleanup_all();
    int fd; SharedData *shm = open_shm(&fd);
    sem_t *sem = sem_open(T_MUTEX, O_CREAT | O_EXCL, 0644, 1);
    assert(sem != SEM_FAILED);

    sem_wait(sem);
    shm->flights[0].step = 7;
    shm->flights[0].x    = 55000.0;
    sem_post(sem);

    sem_wait(sem);
    int    s = shm->flights[0].step;
    double x = shm->flights[0].x;
    sem_post(sem);

    assert(s == 7);
    assert(x == 55000.0);

    sem_close(sem); sem_unlink(T_MUTEX);
    close_shm(shm, fd);
}

/* ════════════════════════════════════════════
 * THREADS
 * ════════════════════════════════════════════ */

/* Thread escreve num inteiro partilhado; pthread_join garante visibilidade */
static void *thread_write99(void *arg){
    *(int *)arg = 99;
    return NULL;
}

static void test_thread_create_and_join(void){
    pthread_t tid;
    int result = 0;

    assert(pthread_create(&tid, NULL, thread_write99, &result) == 0);
    assert(pthread_join(tid, NULL) == 0);
    assert(result == 99);
}

/* 5 threads criadas e joined em ordem: todas terminam */
static void *thread_noop(void *arg){ (void)arg; return NULL; }

static void test_multiple_threads_create_and_join(void){
    const int N = 5;
    pthread_t tids[5];

    for(int i = 0; i < N; i++)
        assert(pthread_create(&tids[i], NULL, thread_noop, NULL) == 0);
    for(int i = 0; i < N; i++)
        assert(pthread_join(tids[i], NULL) == 0);
}

/* Cada thread recebe o seu próprio argumento (malloc por índice) */
static int thread_results[4];

static void *thread_store_index(void *arg){
    int idx = *(int *)arg;
    free(arg);
    thread_results[idx] = idx * 10;
    return NULL;
}

static void test_each_thread_gets_unique_arg(void){
    const int N = 4;
    pthread_t tids[4];
    memset(thread_results, 0, sizeof(thread_results));

    for(int i = 0; i < N; i++){
        int *arg = malloc(sizeof(int));
        assert(arg != NULL);
        *arg = i;
        assert(pthread_create(&tids[i], NULL, thread_store_index, arg) == 0);
    }
    for(int i = 0; i < N; i++)
        pthread_join(tids[i], NULL);

    for(int i = 0; i < N; i++)
        assert(thread_results[i] == i * 10);
}

/* Thread devolve valor heap via void*; chamador liberta */
static void *thread_return_heap(void *arg){
    (void)arg;
    int *r = malloc(sizeof(int));
    assert(r != NULL);
    *r = 42;
    return r;
}

static void test_thread_heap_return_value(void){
    pthread_t tid;
    void *ret;

    assert(pthread_create(&tid, NULL, thread_return_heap, NULL) == 0);
    assert(pthread_join(tid, &ret) == 0);
    assert(*(int *)ret == 42);
    free(ret);
}

/* ════════════════════════════════════════════
 * MUTEX
 * ════════════════════════════════════════════ */

static pthread_mutex_t g_mtx   = PTHREAD_MUTEX_INITIALIZER;
static int             g_count = 0;

static void *mutex_inc(void *arg){
    int n = *(int *)arg;
    free(arg);
    for(int i = 0; i < n; i++){
        pthread_mutex_lock(&g_mtx);
        g_count++;
        pthread_mutex_unlock(&g_mtx);
    }
    return NULL;
}

/* 4 threads incrementam g_count com mutex: resultado deve ser exacto */
static void test_mutex_prevents_race(void){
    g_count = 0;
    const int N = 4, ITERS = 2000;
    pthread_t tids[4];

    for(int i = 0; i < N; i++){
        int *arg = malloc(sizeof(int));
        assert(arg != NULL);
        *arg = ITERS;
        assert(pthread_create(&tids[i], NULL, mutex_inc, arg) == 0);
    }
    for(int i = 0; i < N; i++)
        pthread_join(tids[i], NULL);

    assert(g_count == N * ITERS);
}

/* ════════════════════════════════════════════
 * CONDITION VARIABLES
 * ════════════════════════════════════════════ */

static pthread_mutex_t cv_mtx   = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  cv_cond  = PTHREAD_COND_INITIALIZER;
static int             cv_flag  = 0;
static int             cv_seen  = 0;

static void *cond_waiter(void *arg){
    (void)arg;
    pthread_mutex_lock(&cv_mtx);
    while(!cv_flag)                         /* SEMPRE while — spurious wakeups */
        pthread_cond_wait(&cv_cond, &cv_mtx);
    cv_seen = 1;
    pthread_mutex_unlock(&cv_mtx);
    return NULL;
}

/* pthread_cond_signal acorda uma thread bloqueada em cond_wait */
static void test_cond_signal_unblocks_waiter(void){
    cv_flag = 0; cv_seen = 0;
    pthread_t tid;
    assert(pthread_create(&tid, NULL, cond_waiter, NULL) == 0);

    usleep(20000);  /* garante que a thread entra em cond_wait */

    pthread_mutex_lock(&cv_mtx);
    cv_flag = 1;
    pthread_cond_signal(&cv_cond);
    pthread_mutex_unlock(&cv_mtx);

    assert(pthread_join(tid, NULL) == 0);
    assert(cv_seen == 1);
}

static pthread_mutex_t bc_mtx   = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  bc_cond  = PTHREAD_COND_INITIALIZER;
static int             bc_flag  = 0;
static int             bc_woken = 0;

static void *bc_waiter(void *arg){
    (void)arg;
    pthread_mutex_lock(&bc_mtx);
    while(!bc_flag)
        pthread_cond_wait(&bc_cond, &bc_mtx);
    bc_woken++;
    pthread_mutex_unlock(&bc_mtx);
    return NULL;
}

/* pthread_mutex_init dinâmico + uso + pthread_mutex_destroy */
static void test_mutex_dynamic_init_and_destroy(void){
    pthread_mutex_t mtx;
    assert(pthread_mutex_init(&mtx, NULL) == 0);

    pthread_mutex_lock(&mtx);
    /* secção crítica vazia — apenas valida lock/unlock */
    pthread_mutex_unlock(&mtx);

    assert(pthread_mutex_destroy(&mtx) == 0);
}

/* pthread_cond_broadcast acorda TODAS as threads em espera */
static void test_cond_broadcast_unblocks_all(void){
    bc_flag = 0; bc_woken = 0;
    const int N = 3;
    pthread_t tids[3];

    for(int i = 0; i < N; i++)
        assert(pthread_create(&tids[i], NULL, bc_waiter, NULL) == 0);

    usleep(30000);

    pthread_mutex_lock(&bc_mtx);
    bc_flag = 1;
    pthread_cond_broadcast(&bc_cond);
    pthread_mutex_unlock(&bc_mtx);

    for(int i = 0; i < N; i++)
        pthread_join(tids[i], NULL);

    assert(bc_woken == N);
}

/* ════════════════════════════════════════════
 * BARREIRA COM SEMÁFOROS
 * ════════════════════════════════════════════ */

static sem_t *bar_sem;
static sem_t *bar_nproc;
static int    bar_count = 0;
static int    bar_n     = 4;

static void *barrier_worker(void *arg){
    (void)arg;
    /* padrão da barreira de semáforo (TP10, pp.12-15) */
    sem_wait(bar_nproc);
    bar_count++;
    if(bar_count == bar_n)
        sem_post(bar_sem);   /* último desbloqueia o primeiro */
    sem_post(bar_nproc);

    sem_wait(bar_sem);
    sem_post(bar_sem);       /* cascade: permite que o próximo passe */
    return NULL;
}

/* pthread_cond_init dinâmico + uso + pthread_cond_destroy */
static pthread_mutex_t dyn_mtx;
static pthread_cond_t  dyn_cond;
static int             dyn_flag = 0;

static void *dyn_cond_waiter(void *arg){
    (void)arg;
    pthread_mutex_lock(&dyn_mtx);
    while(!dyn_flag)
        pthread_cond_wait(&dyn_cond, &dyn_mtx);
    pthread_mutex_unlock(&dyn_mtx);
    return NULL;
}

static void test_cond_dynamic_init_and_destroy(void){
    dyn_flag = 0;
    assert(pthread_mutex_init(&dyn_mtx, NULL) == 0);
    assert(pthread_cond_init(&dyn_cond,  NULL) == 0);

    pthread_t tid;
    assert(pthread_create(&tid, NULL, dyn_cond_waiter, NULL) == 0);

    usleep(20000);

    pthread_mutex_lock(&dyn_mtx);
    dyn_flag = 1;
    pthread_cond_signal(&dyn_cond);
    pthread_mutex_unlock(&dyn_mtx);

    assert(pthread_join(tid, NULL) == 0);

    assert(pthread_cond_destroy(&dyn_cond)  == 0);
    assert(pthread_mutex_destroy(&dyn_mtx)  == 0);
}

/* flag já está a 1 quando a thread começa — cond_wait não deve bloquear */
static pthread_mutex_t pre_mtx  = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  pre_cond = PTHREAD_COND_INITIALIZER;
static int             pre_flag = 0;
static int             pre_done = 0;

static void *pre_flag_waiter(void *arg){
    (void)arg;
    pthread_mutex_lock(&pre_mtx);
    while(!pre_flag)
        pthread_cond_wait(&pre_cond, &pre_mtx);
    pre_done = 1;
    pthread_mutex_unlock(&pre_mtx);
    return NULL;
}

static void test_cond_flag_set_before_thread_starts(void){
    pre_done = 0;
    pthread_mutex_lock(&pre_mtx);
    pre_flag = 1;                        /* flag já activa antes do create */
    pthread_mutex_unlock(&pre_mtx);

    pthread_t tid;
    assert(pthread_create(&tid, NULL, pre_flag_waiter, NULL) == 0);
    assert(pthread_join(tid, NULL) == 0);
    assert(pre_done == 1);
}

/* Todas as threads passam a barreira e bar_count == N */
static void test_barrier_releases_all_threads(void){
    cleanup_all();
    bar_count = 0;
    bar_n     = 4;

    bar_sem   = sem_open(T_BARRIER, O_CREAT | O_EXCL, 0644, 0);
    bar_nproc = sem_open(T_NPROC,   O_CREAT | O_EXCL, 0644, 1);
    assert(bar_sem != SEM_FAILED && bar_nproc != SEM_FAILED);

    pthread_t tids[4];
    for(int i = 0; i < bar_n; i++)
        assert(pthread_create(&tids[i], NULL, barrier_worker, NULL) == 0);
    for(int i = 0; i < bar_n; i++)
        assert(pthread_join(tids[i], NULL) == 0);

    assert(bar_count == bar_n);

    sem_close(bar_sem);   sem_unlink(T_BARRIER);
    sem_close(bar_nproc); sem_unlink(T_NPROC);
}

/* Barreira com N=2: caso mínimo — funciona com apenas 2 participantes */
static void test_barrier_two_threads(void){
    cleanup_all();
    bar_count = 0;
    bar_n     = 2;

    bar_sem   = sem_open(T_BARRIER, O_CREAT | O_EXCL, 0644, 0);
    bar_nproc = sem_open(T_NPROC,   O_CREAT | O_EXCL, 0644, 1);
    assert(bar_sem != SEM_FAILED && bar_nproc != SEM_FAILED);

    pthread_t tids[2];
    for(int i = 0; i < bar_n; i++)
        assert(pthread_create(&tids[i], NULL, barrier_worker, NULL) == 0);
    for(int i = 0; i < bar_n; i++)
        assert(pthread_join(tids[i], NULL) == 0);

    assert(bar_count == bar_n);

    sem_close(bar_sem);   sem_unlink(T_BARRIER);
    sem_close(bar_nproc); sem_unlink(T_NPROC);
}

/* ════════════════════════════════════════════
 * MAIN
 * ════════════════════════════════════════════ */
int main(void){
    printf("=== US105 Unit Tests ===\n\n");

    printf("Shared Memory:\n");
    RUN(test_shm_create_and_init);
    RUN(test_shm_exclusive_create);
    RUN(test_shm_flight_slots_readable);
    RUN(test_shm_reopen_after_unlink_is_clean);
    RUN(test_shm_simulation_running_toggle);
    RUN(test_shm_active_flag_independent_per_slot);
    RUN(test_shm_all_max_slots_accessible);

    printf("\nSemaphores:\n");
    RUN(test_sem_mutex_starts_at_1);
    RUN(test_sem_barrier_starts_at_0);
    RUN(test_sem_post_increments);
    RUN(test_sem_multiple_posts_allow_multiple_waits);
    RUN(test_sem_open_existing_without_create);
    RUN(test_sem_guards_shm_write);

    printf("\nThreads:\n");
    RUN(test_thread_create_and_join);
    RUN(test_thread_heap_return_value);
    RUN(test_multiple_threads_create_and_join);
    RUN(test_each_thread_gets_unique_arg);

    printf("\nMutex:\n");
    RUN(test_mutex_prevents_race);
    RUN(test_mutex_dynamic_init_and_destroy);

    printf("\nCondition Variables:\n");
    RUN(test_cond_signal_unblocks_waiter);
    RUN(test_cond_broadcast_unblocks_all);
    RUN(test_cond_dynamic_init_and_destroy);
    RUN(test_cond_flag_set_before_thread_starts);

    printf("\nBarrier (semaphore pattern):\n");
    RUN(test_barrier_releases_all_threads);
    RUN(test_barrier_two_threads);

    printf("\n=== %d/%d tests passed ===\n", tests_passed, tests_run);
    return (tests_passed == tests_run) ? 0 : 1;
}
