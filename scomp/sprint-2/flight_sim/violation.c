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

void check_violations(position_update_t latest[], int n_flights,
                      pid_t pids[], int *violation_count,
                      int max_violations, double h_min, double v_min,
                      FILE *log,
                      int notified[MAX_FLIGHTS][MAX_FLIGHTS],
                      int report_fd_write){
    int i, j, k;

    for(i=0; i<n_flights; i++){
        for(j=i+1; j<n_flights; j++){
            double dx;
            double dy;
            double h_dist;
            double v_dist;
            int in_violation;

            dx = latest[i].x - latest[j].x;
            dy = latest[i].y - latest[j].y;
            h_dist = sqrt(dx * dx + dy * dy);
            v_dist = fabs(latest[i].alt - latest[j].alt);

            in_violation = h_dist < h_min && v_dist < v_min;

            if(in_violation){
                if(!notified[i][j]){
                    fprintf(log,
                            "step=%d flight_1=%d flight_2=%d "
                            "h_dist=%.2f v_dist=%.2f\n",
                            latest[i].step,
                            latest[i].flight_id,
                            latest[j].flight_id,
                            h_dist,
                            v_dist);
                    fflush(log);

                    kill(pids[i], SIGUSR1);
                    kill(pids[j], SIGUSR1);

                    (*violation_count)++;
                    notified[i][j] = 1;

                    emit_violation_event(report_fd_write, &latest[i],
                                         &latest[j], h_dist, v_dist);

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
