#ifndef FLIGHT_SIM_JSON_H
#define FLIGHT_SIM_JSON_H

#include"types.h"

/*
 * JSON parser for the flight simulator, that walks exactly the known
 * wrapper schema:
 *
 *   {
 *     "sim_params": { ... },
 *     "flights": [ { ...flight object... }, ... ]
 *   }
 *
 * Extracts raw values from the text into the structs.
 *
 * Returns 0 on success, non-zero on parse error. On success, *n_flights
 * holds the number of flights parsed and params/flights are populated with
 * the raw values found in the file.
 */
int json_parse_file(const char *path, sim_params_t *params,
                    flight_t flights[], int *n_flights);

#endif
