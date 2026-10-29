package lapr4.lprog.flightdsl.ast;

import java.util.List;

/**
 * A top-level flight block, matching {@code flight : FLIGHT id=FLIGHT_ID LBRACE flightType route leg+ RBRACE}.
 *
 * <p>Each flight carries: a unique identifier (e.g. {@code TP1234A}), its {@link FlightType},
 * a {@link Route} (origin + destination at the flight level), and one or more {@link Leg}s.
 */
public record Flight(
        String identifier,
        FlightType type,
        Route route,
        List<Leg> legs,
        Position position
) { }