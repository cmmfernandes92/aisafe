#include "environment_thread.h"
#include "types.h"
#include <stdlib.h>
#include <unistd.h>
#include <semaphore.h>

/* US110: writes wind into shm every second under sem_mutex */
void* environment_thread(void* arg) {
    env_thread_arg_t *e = (env_thread_arg_t *)arg;
    SharedData *shm = e->shm;
    sem_t      *sem_mutex = e->sem_mutex;
    int running = 1;

    while (running) {

        float speed = rand() % 50;
        float direction = rand() % 360;

        sem_wait(sem_mutex);
        shm->env.wind_speed = speed;
        shm->env.wind_direction = direction;
        running = shm->simulation_running;   /* read flag under the same lock */
        sem_post(sem_mutex);

        if (!running)
            break;

        sleep(1);  // depois podemos melhorar isto
    }

    return NULL;
}