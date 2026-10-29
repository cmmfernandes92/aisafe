#include "detection_thread.h"

#include <math.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

static void closest_separation(const position_update_t *p1_prev,
                               const position_update_t *p1_cur,
                               const position_update_t *p2_prev,
                               const position_update_t *p2_cur,
                               int has_prev,
                               double *h_dist, double *v_dist){
    double dpx, dpy, dpz;
    double dvx, dvy, dvz;
    double dv2;
    double t;
    double x1, y1, z1;
    double x2, y2, z2;
    double ddx, ddy;

    if(!has_prev){
        ddx = p1_cur->x - p2_cur->x;
        ddy = p1_cur->y - p2_cur->y;
        *h_dist = sqrt(ddx * ddx + ddy * ddy);
        *v_dist = fabs(p1_cur->alt - p2_cur->alt);
        return;
    }

    dpx = p1_prev->x - p2_prev->x;
    dpy = p1_prev->y - p2_prev->y;
    dpz = p1_prev->alt - p2_prev->alt;

    dvx = (p1_cur->x - p1_prev->x) - (p2_cur->x - p2_prev->x);
    dvy = (p1_cur->y - p1_prev->y) - (p2_cur->y - p2_prev->y);
    dvz = (p1_cur->alt - p1_prev->alt) - (p2_cur->alt - p2_prev->alt);

    dv2 = dvx * dvx + dvy * dvy + dvz * dvz;
    if(dv2 < 1e-9)
        t = 0.0;
    else
        t = -(dpx * dvx + dpy * dvy + dpz * dvz) / dv2;

    if(t < 0.0)
        t = 0.0;
    if(t > 1.0)
        t = 1.0;

    x1 = p1_prev->x + t * (p1_cur->x - p1_prev->x);
    y1 = p1_prev->y + t * (p1_cur->y - p1_prev->y);
    z1 = p1_prev->alt + t * (p1_cur->alt - p1_prev->alt);
    x2 = p2_prev->x + t * (p2_cur->x - p2_prev->x);
    y2 = p2_prev->y + t * (p2_cur->y - p2_prev->y);
    z2 = p2_prev->alt + t * (p2_cur->alt - p2_prev->alt);

    ddx = x1 - x2;
    ddy = y1 - y2;
    *h_dist = sqrt(ddx * ddx + ddy * ddy);
    *v_dist = fabs(z1 - z2);
}

static void build_violation_event(report_event_t *event,
                                  const position_update_t *first,
                                  const position_update_t *second,
                                  double h_dist, double v_dist){
    memset(event, 0, sizeof(report_event_t));
    event->kind = EV_VIOLATION;
    event->data.violation.viol_type = VIOL_SEPARATION;
    event->data.violation.step = first->step;
    event->data.violation.f1_id = first->flight_id;
    event->data.violation.f2_id = second->flight_id;
    event->data.violation.h_dist = h_dist;
    event->data.violation.v_dist = v_dist;
    event->data.violation.x1 = first->x;
    event->data.violation.y1 = first->y;
    event->data.violation.alt1 = first->alt;
    event->data.violation.vx1 = first->vx;
    event->data.violation.vy1 = first->vy;
    event->data.violation.vz1 = first->vz;
    event->data.violation.x2 = second->x;
    event->data.violation.y2 = second->y;
    event->data.violation.alt2 = second->alt;
    event->data.violation.vx2 = second->vx;
    event->data.violation.vy2 = second->vy;
    event->data.violation.vz2 = second->vz;
}

/* Build a single-flight altitude-bound violation. Encoded into the pairwise
 * row shape so the Java parser is untouched: flight_2 = flight_1 (self), both
 * position/velocity blocks carry the offending flight's state, h_dist = 0 and
 * v_dist = how far past the bound (metres). The kind column distinguishes it. */
static void build_alt_violation_event(report_event_t *event,
                                      const position_update_t *p,
                                      int viol_type, double dist_past){
    memset(event, 0, sizeof(report_event_t));
    event->kind = EV_VIOLATION;
    event->data.violation.viol_type = viol_type;
    event->data.violation.step = p->step;
    event->data.violation.f1_id = p->flight_id;
    event->data.violation.f2_id = p->flight_id;
    event->data.violation.h_dist = 0.0;
    event->data.violation.v_dist = dist_past;
    event->data.violation.x1 = p->x;
    event->data.violation.y1 = p->y;
    event->data.violation.alt1 = p->alt;
    event->data.violation.vx1 = p->vx;
    event->data.violation.vy1 = p->vy;
    event->data.violation.vz1 = p->vz;
    event->data.violation.x2 = p->x;
    event->data.violation.y2 = p->y;
    event->data.violation.alt2 = p->alt;
    event->data.violation.vx2 = p->vx;
    event->data.violation.vy2 = p->vy;
    event->data.violation.vz2 = p->vz;
}

static void copy_shm_positions(detection_context_t *ctx,
                               position_update_t prev[],
                               position_update_t cur[],
                               int active[]){
    int i;

    sem_wait(ctx->sem_mutex);
    for(i=0; i<ctx->n_flights; i++){
        const shm_flight_t *src = &ctx->shm->flights[i];

        active[i] = src->active;
        prev[i].flight_index = i;
        prev[i].flight_id = src->flight_id;
        prev[i].step = src->step - 1;
        prev[i].x = src->prev_x;
        prev[i].y = src->prev_y;
        prev[i].alt = src->prev_alt;
        prev[i].vx = src->vx;
        prev[i].vy = src->vy;
        prev[i].vz = src->vz;

        cur[i].flight_index = i;
        cur[i].flight_id = src->flight_id;
        cur[i].step = src->step;
        cur[i].x = src->x;
        cur[i].y = src->y;
        cur[i].alt = src->alt;
        cur[i].vx = src->vx;
        cur[i].vy = src->vy;
        cur[i].vz = src->vz;
    }
    sem_post(ctx->sem_mutex);
}

/* Predictive conflict warning; for each pair of active flights 
 * predict both positions forward at constant per-step velocity. 
 * If separation is predicted to be below safety trheshold. print the
 * earliest predicted step. */
static void lookahead_warn(detection_context_t *ctx,
                           const position_update_t cur[], const int active[],
                           int step){
    int i, j;

    if(ctx->lookahead <= 0)
        return;

    for(i=0; i<ctx->n_flights; i++){
        for(j=i+1; j<ctx->n_flights; j++){
            double dpx, dpy, dpz;
            double dvx, dvy, dvz;
            double dv2;
            double t;
            double hd;
            double vd;

            if(!active[i] || !active[j])
                continue;

            /* relative position and per-step relative velocity */
            dpx = cur[i].x - cur[j].x;
            dpy = cur[i].y - cur[j].y;
            dpz = cur[i].alt - cur[j].alt;
            dvx = cur[i].vx - cur[j].vx;
            dvy = cur[i].vy - cur[j].vy;
            dvz = cur[i].vz - cur[j].vz;

            dv2 = dvx * dvx + dvy * dvy + dvz * dvz;
            if(dv2 < 1e-9)
                continue;   /* not closing (parallel / co-moving) */

            t = -(dpx * dvx + dpy * dvy + dpz * dvz) / dv2;
            if(t <= 0.0 || t > (double)ctx->lookahead)
                continue;   /* CPA in the past/now, or beyond the horizon */

            hd = hypot(dpx + t * dvx, dpy + t * dvy);
            vd = fabs(dpz + t * dvz);
            if(hd < ctx->h_min && vd < ctx->v_min){
                char buf[256];
                int n;

                n = snprintf(buf, sizeof(buf),
                             "[Lookahead] step %d: flights %d and %d predicted to "
                             "violate in %.1f step(s) (h=%.1f v=%.1f)\n",
                             step, cur[i].flight_id, cur[j].flight_id, t, hd, vd);
                if(n > 0)
                    write(STDERR_FILENO, buf, (size_t)n);
            }
        }
    }
}

static void scan_shared_memory_for_violations(detection_context_t *ctx,
                                              int step){
    position_update_t prev[MAX_FLIGHTS];
    position_update_t cur[MAX_FLIGHTS];
    int active[MAX_FLIGHTS];
    int i, j, k;
    int has_prev;

    memset(prev, 0, sizeof(prev));
    memset(cur, 0, sizeof(cur));
    memset(active, 0, sizeof(active));
    copy_shm_positions(ctx, prev, cur, active);  /* US107 AC1 */
    has_prev = step > 0;

    /* Single-flight airspace altitude-bound check: an active flight above the
       ceiling or below the floor is a safety violation. Reported once per flight via
       alt_notified if its a sustained breach. */
    for(i=0; i<ctx->n_flights; i++){
        int breach_type;
        double dist_past;

        if(!active[i])
            continue;

        breach_type = 0;
        dist_past = 0.0;
        if(cur[i].alt > ctx->area_alt_max){
            breach_type = VIOL_ALT_CEILING;
            dist_past = cur[i].alt - ctx->area_alt_max;
        }
        else if(cur[i].alt < ctx->area_alt_min){
            breach_type = VIOL_ALT_FLOOR;
            dist_past = ctx->area_alt_min - cur[i].alt;
        }

        if(breach_type == 0){
            ctx->alt_notified[i] = 0;
            continue;
        }
        if(ctx->alt_notified[i])
            continue;   /* already reported this breach */

        {
            report_event_t event;

            kill(ctx->pids[i], SIGUSR1);   /* notify the offending flight */
            ctx->violation_count++;
            ctx->alt_notified[i] = 1;

            build_alt_violation_event(&event, &cur[i], breach_type, dist_past);
            if(enqueue_report_event(ctx->report_queue, &event) != 0)
                perror("enqueue altitude violation event");

            if(ctx->violation_count > ctx->max_violations){
                ctx->threshold_exceeded = 1;
                for(k=0; k<ctx->n_flights; k++)
                    kill(ctx->pids[k], SIGTERM);
            }
        }
    }

    for(i=0; i<ctx->n_flights; i++){
        for(j=i+1; j<ctx->n_flights; j++){
            double h_dist;
            double v_dist;
            int in_violation;

            /* a flight that has not yet entered the area (active==0) holds at
               its entry point and must not generate violations */
            if(!active[i] || !active[j])
                continue;

            closest_separation(&prev[i], &cur[i], &prev[j], &cur[j],
                               has_prev, &h_dist, &v_dist);
            in_violation = h_dist < ctx->h_min && v_dist < ctx->v_min;

            if(in_violation){
                if(!ctx->notified[i][j]){
                    report_event_t event;

                    /* US107 AC2: notify flight processes via SIGUSR1. */

                    kill(ctx->pids[i], SIGUSR1);
                    kill(ctx->pids[j], SIGUSR1);

                    ctx->violation_count++;
                    ctx->notified[i][j] = 1;

                    build_violation_event(&event, &cur[i], &cur[j],
                                          h_dist, v_dist);

                    /* US107 AC2+AC3+AC4: enqueue pushes state under mutex
                     * then signals the report thread's condition variable.
                     * See enqueue_report_event() in report_thread.c. */

                    if(enqueue_report_event(ctx->report_queue, &event) != 0)
                        perror("enqueue violation event");

                    if(ctx->violation_count > ctx->max_violations){
                        ctx->threshold_exceeded = 1;
                        for(k=0; k<ctx->n_flights; k++)
                            kill(ctx->pids[k], SIGTERM);
                    }
                }
            }
            else{
                ctx->notified[i][j] = 0;
            }
        }
    }

    /* predictive warning for pairs going toward a violation (console only) */
    lookahead_warn(ctx, cur, active, step);
}

void *violation_detection_thread(void *arg){
    detection_context_t *ctx;

    ctx = (detection_context_t *)arg;

    while(1){
        int step;

        /* US107 AC1: wait for the parent to tell us a new step is ready. */
        pthread_mutex_lock(&ctx->mutex);
        while(!ctx->step_ready && !ctx->simulation_done)
            pthread_cond_wait(&ctx->step_ready_cond, &ctx->mutex);

        if(ctx->simulation_done && !ctx->step_ready){
            pthread_mutex_unlock(&ctx->mutex);
            break;
        }

        step = ctx->current_step;
        ctx->step_ready = 0;
        pthread_mutex_unlock(&ctx->mutex);

        /* US107 AC1: scan shm for violations this step. */
        scan_shared_memory_for_violations(ctx, step);

        /* US107 / US108: signal parent that this step's detection is done. */
        pthread_mutex_lock(&ctx->mutex);
        ctx->step_processed = 1;
        pthread_cond_signal(&ctx->step_done_cond);
        pthread_mutex_unlock(&ctx->mutex);
    }

    return NULL;
}

int detection_context_init(detection_context_t *ctx, SharedData *shm,
                                  sem_t *sem_mutex, pid_t pids[],
                                  const sim_params_t *params,
                                  report_queue_t *report_queue){
    memset(ctx, 0, sizeof(*ctx));
    ctx->shm = shm;
    ctx->sem_mutex = sem_mutex;
    ctx->pids = pids;
    ctx->n_flights = params->n_flights;
    ctx->max_violations = params->max_violations;
    ctx->h_min = params->h_min;
    ctx->v_min = params->v_min;
    ctx->area_alt_min = params->area_alt_min;
    ctx->area_alt_max = params->area_alt_max;
    ctx->lookahead = params->history_depth; 
    ctx->report_queue = report_queue;

    if(pthread_mutex_init(&ctx->mutex, NULL) != 0)
        return -1;
    if(pthread_cond_init(&ctx->step_ready_cond, NULL) != 0){
        pthread_mutex_destroy(&ctx->mutex);
        return -1;
    }
    if(pthread_cond_init(&ctx->step_done_cond, NULL) != 0){
        pthread_cond_destroy(&ctx->step_ready_cond);
        pthread_mutex_destroy(&ctx->mutex);
        return -1;
    }

    return 0;
}

void detection_context_destroy(detection_context_t *ctx){
    pthread_cond_destroy(&ctx->step_done_cond);
    pthread_cond_destroy(&ctx->step_ready_cond);
    pthread_mutex_destroy(&ctx->mutex);
}

void detection_context_stop(detection_context_t *ctx){
    pthread_mutex_lock(&ctx->mutex);
    ctx->simulation_done = 1;
    pthread_cond_broadcast(&ctx->step_ready_cond);
    pthread_mutex_unlock(&ctx->mutex);
}

