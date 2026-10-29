#define _POSIX_C_SOURCE 200809L
#include "types.h"
#include <stdio.h>
#include <stdlib.h>

int main() {

    SharedData shm;

    shm.env.wind_speed = 25.0;
    shm.env.wind_direction = 180.0;

    if (shm.env.wind_speed != 25.0) {
        printf("FAIL: wind_speed\n");
        return 1;
    }

    if (shm.env.wind_direction != 180.0) {
        printf("FAIL: wind_direction\n");
        return 1;
    }

    printf("PASS: US110 basic structure test\n");
    return 0;
}
