#include"types.h"

#include<stdio.h>
#include<string.h>

static void set_defaults(sim_params_t *params, const char *scenario){
    memset(params, 0, sizeof(sim_params_t));

    params->total_steps = 6;
    params->max_violations = 3;
    params->area_x_min = -1000.0;
    params->area_x_max = 1000.0;
    params->area_y_min = -1000.0;
    params->area_y_max = 1000.0;
    params->area_alt_min = 0.0;
    params->area_alt_max = 20000.0;
    params->h_min = H_MIN;
    params->v_min = V_MIN;

    strncpy(params->scenario, scenario, SCENARIO_NAME_SIZE - 1);
    params->scenario[SCENARIO_NAME_SIZE - 1] = '\0';
}

static void set_flight(flight_t *flight, int index, int id,
                       double x, double y, double alt,
                       double vx, double vy, double vz){
    flight->index = index;
    flight->id = id;
    flight->x = x;
    flight->y = y;
    flight->alt = alt;
    flight->vx = vx;
    flight->vy = vy;
    flight->vz = vz;
}

static void load_clean(sim_params_t *params, flight_t flights[]){
    set_defaults(params, "clean");
    params->n_flights = 3;
    params->total_steps = 6;
    params->max_violations = 3;

    set_flight(&flights[0], 0, 101, 0.0, 0.0, 10000.0, 10.0, 0.0, 0.0);
    set_flight(&flights[1], 1, 102, 0.0, 50.0, 10000.0, 10.0, 0.0, 0.0);
    set_flight(&flights[2], 2, 103, 0.0, 0.0, 11000.0, 10.0, 0.0, 0.0);
}

static void load_violation(sim_params_t *params, flight_t flights[]){
    set_defaults(params, "violation");
    params->n_flights = 3;
    params->total_steps = 6;
    params->max_violations = 10;

    set_flight(&flights[0], 0, 201, 0.0, 0.0, 10000.0, 3.0, 0.0, 0.0);
    set_flight(&flights[1], 1, 202, 20.0, 0.0, 10000.0, -3.0, 0.0, 0.0);
    set_flight(&flights[2], 2, 203, 0.0, 100.0, 12000.0, 1.0, 0.0, 0.0);
}

static void load_threshold(sim_params_t *params, flight_t flights[]){
    set_defaults(params, "threshold");
    params->n_flights = 3;
    params->total_steps = 8;
    params->max_violations = 0;

    set_flight(&flights[0], 0, 301, 0.0, 0.0, 10000.0, 3.0, 0.0, 0.0);
    set_flight(&flights[1], 1, 302, 20.0, 0.0, 10000.0, -3.0, 0.0, 0.0);
    set_flight(&flights[2], 2, 303, 0.0, 100.0, 12000.0, 1.0, 0.0, 0.0);
}

int load_config_from_json(const char *scenario, sim_params_t *params,
                          flight_t flights[]){
    const char *selected;

    selected = scenario;
    if(selected == NULL || selected[0] == '\0')
        selected = "clean";

    if(strcmp(selected, "clean") == 0){
        load_clean(params, flights);
        return 0;
    }
    if(strcmp(selected, "violation") == 0){
        load_violation(params, flights);
        return 0;
    }
    if(strcmp(selected, "threshold") == 0){
        load_threshold(params, flights);
        return 0;
    }

    fprintf(stderr, "Unknown scenario '%s'. Use: clean, violation, threshold\n",
            selected);
    return 1;
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

    for(i=0; i<params->n_flights; i++){
        if(flights[i].index != i){
            fprintf(stderr, "Invalid flight index for flight %d\n",
                    flights[i].id);
            return 6;
        }
        if(flights[i].vx == 0.0 &&
           flights[i].vy == 0.0 &&
           flights[i].vz == 0.0){
            fprintf(stderr, "Flight %d has zero velocity vector\n",
                    flights[i].id);
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
