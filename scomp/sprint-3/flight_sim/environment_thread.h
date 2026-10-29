#ifndef ENVIRONMENT_THREAD_H
#define ENVIRONMENT_THREAD_H

#include "types.h"

/* shm + mutex passed to the env thread */
typedef struct {
    SharedData *shm;
    sem_t      *sem_mutex;
} env_thread_arg_t;

void* environment_thread(void* arg);

#endif