package lapr4.lprog.flightdsl.ast;

import java.util.List;

/**
 * A leg of a flight. The grammar enforces exactly one departure, one arrival, one fuel
 * block, and one segments block per leg, so each field is single-valued; the semantic
 * validator still surfaces defensive errors if any of them is null (this only happens
 * when an AST is constructed programmatically outside the parser, e.g. in tests).
 */
public record Leg(
        int number,
        Departure departure,
        Arrival arrival,
        Fuel fuel,
        List<Segment> segments,
        Position position
) { }
