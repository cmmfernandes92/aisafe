#include"types.h"

#include<math.h>
#include<stdio.h>
#include<signal.h>

static void emit_violation_event(int report_fd_write,
                                 const position_update_t *first,
                                 const position_update_t *second,
                                 double h_dist, double v_dist){
    report_event_t event;

    event.kind = EV_VIOLATION;
    event.data.violation.step = first->step;
    event.data.violation.f1_id = first->flight_id;
    event.data.violation.f2_id = second->flight_id;
    event.data.violation.h_dist = h_dist;
    event.data.violation.v_dist = v_dist;
    event.data.violation.x1 = first->x;
    event.data.violation.y1 = first->y;
    event.data.violation.alt1 = first->alt;
    event.data.violation.vx1 = first->vx;
    event.data.violation.vy1 = first->vy;
    event.data.violation.vz1 = first->vz;
    event.data.violation.x2 = second->x;
    event.data.violation.y2 = second->y;
    event.data.violation.alt2 = second->alt;
    event.data.violation.vx2 = second->vx;
    event.data.violation.vy2 = second->vy;
    event.data.violation.vz2 = second->vz;

    if(write_report_event(report_fd_write, &event) != 0)
        perror("write report violation");
}

/*
 * Closest distance between two flights during one step.
 * On step 0 (no prev) we just use the current positions.
 * Otherwise we find the time t in [0,1] where they are nearest
 * and mesure the horizontal and vertical gap there.
 */
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
        t = 0.0;                /* parallel paths: closest at start */
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

void check_violations(const position_update_t prev[],
                      const position_update_t cur[], int n_flights,
                      int has_prev,
                      pid_t pids[], int *violation_count,
                      int max_violations, double h_min, double v_min,
                      FILE *log,
                      int notified[MAX_FLIGHTS][MAX_FLIGHTS],
                      int report_fd_write){
    int i, j, k;

    for(i=0; i<n_flights; i++){
        for(j=i+1; j<n_flights; j++){
            double h_dist;
            double v_dist;
            int in_violation;

            closest_separation(&prev[i], &cur[i], &prev[j], &cur[j],
                               has_prev, &h_dist, &v_dist);

            in_violation = h_dist < h_min && v_dist < v_min;

            if(in_violation){
                if(!notified[i][j]){
                    fprintf(log,
                            "step=%d flight_1=%d flight_2=%d "
                            "h_dist=%.2f v_dist=%.2f\n",
                            cur[i].step,
                            cur[i].flight_id,
                            cur[j].flight_id,
                            h_dist,
                            v_dist);
                    fflush(log);

                    kill(pids[i], SIGUSR1);
                    kill(pids[j], SIGUSR1);

                    (*violation_count)++;
                    notified[i][j] = 1;

                    emit_violation_event(report_fd_write, &cur[i],
                                         &cur[j], h_dist, v_dist);

                    if(*violation_count > max_violations){
                        for(k=0; k<n_flights; k++)
                            kill(pids[k], SIGTERM);
                    }
                }
            }
            else{
                notified[i][j] = 0;
            }
        }
    }
}