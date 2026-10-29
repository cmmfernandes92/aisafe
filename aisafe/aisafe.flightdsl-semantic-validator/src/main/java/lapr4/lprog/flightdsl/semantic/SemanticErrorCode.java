package lapr4.lprog.flightdsl.semantic;

/**
 * Stable identifiers for the semantic findings emitted by {@link FlightPlanSemanticValidator}.
 * Each constant maps directly to a bullet of §3.4.5 of the project requirements.
 */
public enum SemanticErrorCode {

    // Flight-level
    FLIGHT_ID_NOT_UNIQUE,
    FLIGHT_TYPE_INVALID,

    // Leg-level
    FLIGHT_MISSING_LEGS,
    LEG_MISSING_DEPARTURE,
    LEG_MISSING_ARRIVAL,
    LEG_MISSING_SEGMENTS,
    LEG_MISSING_FUEL,
    FUEL_NOT_POSITIVE,

    // Leg sequence coherence
    LEG_SEQUENCE_AIRPORT_MISMATCH,
    LEG_SEQUENCE_TIME_NOT_ORDERED,

    // Route coherence
    ROUTE_ORIGIN_MISMATCH,
    ROUTE_DESTINATION_MISMATCH,
    ROUTE_AIRPORT_REPEATED,

    // Segment coherence
    SEGMENT_START_EQUALS_END,

    // Domain consistency
    AIRPORT_CODE_BAD_FORMAT,
    NUMERIC_NOT_POSITIVE,
    DATETIME_INVALID_CALENDAR
}