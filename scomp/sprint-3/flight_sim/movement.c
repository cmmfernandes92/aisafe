/**
 * Movement / kinematics: projection, profile lookup, per-step advance.
 * Scope: movement only. Aerodynamics is out of scope (LPROG / LAPR4).
 **/

#include"types.h"
#include"movement.h"

#include<math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define DEG_TO_RAD (M_PI / 180.0)
#define KNOTS_TO_MS 0.514444

void project_to_local(double lat, double lon,
                     double ref_lat, double ref_lon,
                     double *x, double *y){
    double lat_rad;
    double ref_lat_rad;

    lat_rad = lat * DEG_TO_RAD;
    ref_lat_rad = ref_lat * DEG_TO_RAD;

    /* east-west distance shrinks with latitude (cos of mean latitude) */
    *x = EARTH_RADIUS_M * (lon - ref_lon) * DEG_TO_RAD *
         cos((lat_rad + ref_lat_rad) / 2.0);
    *y = EARTH_RADIUS_M * (lat - ref_lat) * DEG_TO_RAD;
}

double haversine_m(double lat1, double lon1, double lat2, double lon2){
    double dlat;
    double dlon;
    double a;
    double c;

    dlat = (lat2 - lat1) * DEG_TO_RAD;
    dlon = (lon2 - lon1) * DEG_TO_RAD;

    a = sin(dlat / 2.0) * sin(dlat / 2.0) +
        cos(lat1 * DEG_TO_RAD) * cos(lat2 * DEG_TO_RAD) *
        sin(dlon / 2.0) * sin(dlon / 2.0);
    c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a));

    return EARTH_RADIUS_M * c;
}

void profile_lookup(const flight_t *flight, double alt, int is_climb,
                   double *rate_ms, double *speed_ms){
    int i;
    int best;
    double best_diff;
    const profile_point_t *table;
    int n;

    if(is_climb){
        table = flight->climb;
        n = flight->n_climb;
    }
    else{
        table = flight->descend;
        n = flight->n_descend;
    }

    if(n <= 0){
        *rate_ms = 0.0;
        *speed_ms = 0.0;
        return;
    }

    /* pick the band whose altitude is closest to the current altitude */
    best = 0;
    best_diff = fabs(table[0].altitude - alt);
    for(i=1; i<n; i++){
        double diff;

        diff = fabs(table[i].altitude - alt);
        if(diff < best_diff){
            best_diff = diff;
            best = i;
        }
    }

    *rate_ms = table[best].rate;
    *speed_ms = table[best].speed_knots * KNOTS_TO_MS;
}

static int current_segment(const flight_t *flight){
    int idx;

    idx = flight->seg_index;
    if(idx < 0)
        idx = 0;
    if(idx >= flight->n_segments)
        idx = flight->n_segments - 1;
    return idx;
}

void advance_flight(flight_t *flight, double dt,
                   double wind_x, double wind_y){
    int idx;
    const segment_t *seg;
    double tx;
    double ty;
    double dx;
    double dy;
    double horiz_dist;
    double rate_ms;
    double speed_ms;
    double air_vx;
    double air_vy;
    double ground_vx;
    double ground_vy;
    int is_climb;

    if(flight->n_segments <= 0)
        return;

    /* A flight that reached its final waypoint holds position: and
     *  do not advance. The  "arrived" state is what removes the flight 
     *  from detection (flight_proc marks it inactive in shm). */
    if(flight->arrived){
        flight->vx = 0.0;
        flight->vy = 0.0;
        flight->vz = 0.0;
        return;
    }

    idx = current_segment(flight);
    seg = &flight->segments[idx];

    tx = seg->end_x;
    ty = seg->end_y;

    dx = tx - flight->x;
    dy = ty - flight->y;
    horiz_dist = sqrt(dx * dx + dy * dy);

    /* climbing if the segment target altitude is above current */
    is_climb = seg->end_alt >= flight->alt;
    profile_lookup(flight, flight->alt, is_climb, &rate_ms, &speed_ms);

    /* horizontal air velocity points along the segment bearing */
    if(horiz_dist > 1e-6){
        air_vx = speed_ms * (dx / horiz_dist);
        air_vy = speed_ms * (dy / horiz_dist);
    }
    else{
        air_vx = 0.0;
        air_vy = 0.0;
    }

    /* ground velocity = air velocity + wind */
    ground_vx = air_vx + wind_x;
    ground_vy = air_vy + wind_y;

    flight->x += ground_vx * dt;
    flight->y += ground_vy * dt;
    flight->alt += rate_ms * dt;

    /* clamp altitude to the segment target so we don't overshoot */
    if(is_climb && flight->alt > seg->end_alt)
        flight->alt = seg->end_alt;
    if(!is_climb && flight->alt < seg->end_alt)
        flight->alt = seg->end_alt;

    /* derived velocity (metres per step) for the mid-step check */
    flight->vx = ground_vx * dt;
    flight->vy = ground_vy * dt;
    flight->vz = rate_ms * dt;

    /* reached segment end horizontally */
    dx = tx - flight->x;
    dy = ty - flight->y;
    if(sqrt(dx * dx + dy * dy) < speed_ms * dt){
        if(flight->seg_index < flight->n_segments - 1){
            /* advance to the next segment */
            flight->seg_index++;
        }
        else{
            /* final waypoint reached: clamp exactly to the endpoint, stop, and
             * mark arrived so the flight holds here for the rest of the sim. */
            flight->x = tx;
            flight->y = ty;
            flight->alt = seg->end_alt;
            flight->vx = 0.0;
            flight->vy = 0.0;
            flight->vz = 0.0;
            flight->arrived = 1;
        }
    }
}
