package lapr4.lprog.flightdsl.ast;

/**
 * Route block at flight level, matching the grammar's {@code route} rule which only
 * carries an origin and a destination airport. The "no repeated airports" semantic rule
 * is checked across the full leg sequence (since the grammar does not model intermediate
 * waypoints inside the route block itself).
 */
public record Route(AirportRef origin, AirportRef destination, Position position) { }