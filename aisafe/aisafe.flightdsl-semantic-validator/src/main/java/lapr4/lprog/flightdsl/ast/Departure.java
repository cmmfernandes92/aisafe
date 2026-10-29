package lapr4.lprog.flightdsl.ast;

/** Departure block inside a leg: airport + datetime. */
public record Departure(AirportRef airport, TemporalLiteral when, Position position) { }