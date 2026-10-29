package lapr4.lprog.flightdsl.ast;

/**
 * A length value in metres, matching {@code length : value=number M_UNIT}.
 * Used for altitude and width inside slots.
 */
public record Length(double value, Position position) { }