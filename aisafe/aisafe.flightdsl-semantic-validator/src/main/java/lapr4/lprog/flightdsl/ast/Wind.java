package lapr4.lprog.flightdsl.ast;

/**
 * Wind block inside a segment. Direction is an angle in degrees relative to North;
 * speed is in m/s (the grammar requires {@code MS_UNIT} after the speed value).
 * Per §3.4.5, only the speed has an explicit positivity requirement.
 */
public record Wind(double direction, double speed, Position position) { }