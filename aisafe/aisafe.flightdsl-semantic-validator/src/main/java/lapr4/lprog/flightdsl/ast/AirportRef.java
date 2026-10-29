package lapr4.lprog.flightdsl.ast;

/**
 * Reference to an airport by its IATA (3-letter) or ICAO (4-letter) code.
 * Format validation is performed by {@code DomainConsistencyRule} (§3.4.5).
 */
public record AirportRef(String code, Position position) { }