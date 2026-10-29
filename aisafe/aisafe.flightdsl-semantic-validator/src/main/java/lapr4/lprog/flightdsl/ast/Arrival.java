package lapr4.lprog.flightdsl.ast;

/** Arrival block inside a leg: airport + datetime. */
public record Arrival(AirportRef airport, TemporalLiteral when, Position position) { }