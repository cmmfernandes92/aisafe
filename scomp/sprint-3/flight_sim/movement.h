#ifndef FLIGHT_SIM_MOVEMENT_H
#define FLIGHT_SIM_MOVEMENT_H

#include"types.h"

/*
 * Movement / flight kinematics.
 *
 * Coordinates: equirectangular projection around ref_lat/ref_lon. Flat-earth
 * approximation, valid for the local sim area, keeps the violation check in
 * fast Cartesian distance. Haversine formula: standard navigation math.
 */

#define EARTH_RADIUS_M 6371000.0

/* lat/lon (deg) -> local x (east), y (north) in metres, relative to ref. */
void project_to_local(double lat, double lon,
                     double ref_lat, double ref_lon,
                     double *x, double *y);

/* Great-circle distance in metres.  */
double haversine_m(double lat1, double lon1, double lat2, double lon2);

/* Closest altitude band: writes vertical rate (m/s) and air speed (m/s). */
void profile_lookup(const flight_t *flight, double alt, int is_climb,
                   double *rate_ms, double *speed_ms);

/* Advance one step of dt seconds. Wind (m/s) added to ground speed; 0,0 = none. */
void advance_flight(flight_t *flight, double dt,
                   double wind_x, double wind_y);

#endif
