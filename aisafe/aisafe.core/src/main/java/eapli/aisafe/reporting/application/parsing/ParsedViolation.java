package eapli.aisafe.reporting.application.parsing;

import eapli.aisafe.flightmanagement.domain.FlightId;

/**
 * Format-free DTO for one {@code [violations]} row: the simulation step, the two
 * surrogate ids and their sidecar-resolved identities, and a position + velocity
 * vector for each aircraft. No longer assumes the event is pairwise — the trailing
 * {@code kind} discriminates a loss of separation from an altitude ceiling/floor
 * bust (where only the first aircraft is meaningful).
 */
public record ParsedViolation(
        int step,
        int flight1,
        int flight2,
        double horizontalDistance,
        double verticalDistance,
        double x1, double y1, double alt1, double vx1, double vy1, double vz1,
        double x2, double y2, double alt2, double vx2, double vy2, double vz2,
        FlightId identity1,
        FlightId identity2,
        String kind) {
}