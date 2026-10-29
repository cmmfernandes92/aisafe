package lapr4.lprog.flightdsl.ast;

import java.util.List;

/**
 * Root AST node, matching {@code flightPlanFile : flight+ EOF}.
 *
 * <p>A single source file may declare one or more flights; the validator enforces that
 * their identifiers are unique within the file (§3.4.5 Flight-level rules).
 */
public record FlightPlanFile(List<Flight> flights, Position position) { }