#include"types.h"
#include"json.h"
#include"movement.h"

#include<stdio.h>
#include<string.h>

/*
 * PRovide params with the defaults the JSON may override. Anything missing keeps the default.
 */
static void set_defaults(sim_params_t *params){
    memset(params, 0, sizeof(sim_params_t));

    params->total_steps = 6;
    params->max_violations = 3;
    params->area_x_min = -500000.0;
    params->area_x_max = 500000.0;
    params->area_y_min = -500000.0;
    params->area_y_max = 500000.0;
    params->area_alt_min = 0.0;
    params->area_alt_max = 20000.0;
    params->h_min = H_MIN;
    params->v_min = V_MIN;
    params->seconds_per_step = 60.0;
    params->history_depth = 2;
    params->ref_lat = 0.0;
    params->ref_lon = 0.0;

    strncpy(params->scenario, "unnamed", SCENARIO_NAME_SIZE - 1);
    params->scenario[SCENARIO_NAME_SIZE - 1] = '\0';
}

/*
 * Project flight segments from geographic coords to local metres and get
 * the flight's starting position from the first segment's start point.
 */
static void project_flight(flight_t *flight, double ref_lat, double ref_lon){
    int s;

    for(s=0; s<flight->n_segments; s++){
        segment_t *seg;

        seg = &flight->segments[s];
        project_to_local(seg->start_lat, seg->start_lon, ref_lat, ref_lon,
                         &seg->start_x, &seg->start_y);
        project_to_local(seg->end_lat, seg->end_lon, ref_lat, ref_lon,
                         &seg->end_x, &seg->end_y);
    }

    if(flight->n_segments > 0){
        flight->x = flight->segments[0].start_x;
        flight->y = flight->segments[0].start_y;
        flight->alt = flight->segments[0].start_alt;
    }
    flight->seg_index = 0;
}

int load_config_from_json(const char *path, sim_params_t *params,
                          flight_t flights[]){
    int n_flights;
    int i;

    set_defaults(params);

    n_flights = 0;
    if(json_parse_file(path, params, flights, &n_flights) != 0){
        fprintf(stderr, "Failed to parse scenario file '%s'\n", path);
        return 1;
    }

    params->n_flights = n_flights;

    for(i=0; i<params->n_flights; i++)
        project_flight(&flights[i], params->ref_lat, params->ref_lon);

    return validate_config(params, flights);
}

int validate_config(const sim_params_t *params, const flight_t flights[]){
    int i, j;

    if(params->n_flights <= 0 || params->n_flights > MAX_FLIGHTS){
        fprintf(stderr, "Invalid n_flights: %d\n", params->n_flights);
        return 1;
    }
    if(params->total_steps <= 0 || params->total_steps > HISTORY_SIZE){
        fprintf(stderr, "Invalid total_steps: %d\n", params->total_steps);
        return 2;
    }
    if(params->max_violations < 0){
        fprintf(stderr, "Invalid max_violations: %d\n",
                params->max_violations);
        return 3;
    }
    if(params->area_x_min >= params->area_x_max ||
       params->area_y_min >= params->area_y_max ||
       params->area_alt_min >= params->area_alt_max){
        fprintf(stderr, "Invalid simulation area bounds\n");
        return 4;
    }
    if(params->h_min <= 0.0 || params->v_min <= 0.0){
        fprintf(stderr, "Invalid safety thresholds\n");
        return 5;
    }
    if(params->seconds_per_step <= 0.0){
        fprintf(stderr, "Invalid seconds_per_step: %.2f\n",
                params->seconds_per_step);
        return 10;
    }
    if(params->history_depth < 2 || params->history_depth > MAX_HISTORY){
        fprintf(stderr, "Invalid history_depth: %d (must be 2..%d)\n",
                params->history_depth, MAX_HISTORY);
        return 11;
    }

    for(i=0; i<params->n_flights; i++){
        if(flights[i].index != i){
            fprintf(stderr, "Invalid flight index for flight %d\n",
                    flights[i].id);
            return 6;
        }
        if(flights[i].n_segments <= 0){
            fprintf(stderr, "Flight %d has no segments\n", flights[i].id);
            return 7;
        }
        if(flights[i].x < params->area_x_min ||
           flights[i].x > params->area_x_max ||
           flights[i].y < params->area_y_min ||
           flights[i].y > params->area_y_max ||
           flights[i].alt < params->area_alt_min ||
           flights[i].alt > params->area_alt_max){
            fprintf(stderr, "Flight %d starts outside the area\n",
                    flights[i].id);
            return 8;
        }
        for(j=i+1; j<params->n_flights; j++){
            if(flights[i].id == flights[j].id){
                fprintf(stderr, "Duplicate flight ID: %d\n", flights[i].id);
                return 9;
            }
        }
    }

    return 0;
}