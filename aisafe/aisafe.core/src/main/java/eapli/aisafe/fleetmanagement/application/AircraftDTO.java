package eapli.aisafe.fleetmanagement.application;

/**
 * US078 — Data Transfer Object for a fleet entry crossing the TCP wire.
 * <p>
 * {@link ListCompanyFleetController} returns {@code FleetEntry} (an Aircraft +
 * AircraftModel record). Converting to this DTO prevents domain objects from
 * crossing the process boundary, decouples the remote client from domain
 * internals, and isolates serialisation logic — per EAPLI TP9 (DTO pattern).
 * <p>
 * Placed in {@code fleetmanagement.application} (same reasoning as
 * {@code AssignedFlightDTO} in pilot management) so both the server-side
 * session context and any future consumers share the same shape.
 */
public record AircraftDTO(
        String registration,
        String modelName,
        String status,
        int capacity) {
}
