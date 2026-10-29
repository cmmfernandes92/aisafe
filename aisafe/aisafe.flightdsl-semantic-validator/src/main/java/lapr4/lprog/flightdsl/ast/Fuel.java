package lapr4.lprog.flightdsl.ast;

/**
 * Fuel block, matching {@code fuel : FUEL COLON quantity=number unit=fuelUnit}.
 *
 * <p>The grammar accepts negative numeric literals, so the strict-positivity requirement
 * (§3.4.5 Leg-level) must be enforced here by the semantic validator.
 */
public record Fuel(double quantity, FuelUnit unit, Position position) { }
