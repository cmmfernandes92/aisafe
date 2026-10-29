package eapli.aisafe.reporting.application.parsing;

import eapli.aisafe.flightmanagement.domain.FlightId;

/**
 * Format-free DTO for one {@code [flight_statuses]} row. {@code identity} is the
 * domain flight resolved from the {@code flights.csv} sidecar by surrogate id, or
 * {@code null} when the run folder carried no mapping for it.
 */
public record ParsedFlightStatus(int flightId, String outcome, int exitCode, FlightId identity) {
}