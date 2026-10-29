package eapli.aisafe.flightmanagement.domain.physics;

import java.util.List;

/**
 * The read-model produced by the US085 simulation: everything the console report
 * and the browser simulator need to render a real, weather-resolved, physics-tested
 * flight plan. It carries the plan identity and times, the loaded/trip/required
 * fuel, the per-segment resolved winds, the §3.3 aircraft physics (so the browser
 * reads its constants from the domain rather than a hard-coded table), the fuel
 * timeline, and the test verdict.
 *
 * <p>It is produced read-only (no plan-state change). The state transition
 * {@code VALIDATED → TESTED} is driven separately by {@code Flight.testCurrentPlan}
 * only when {@link #verdict()} passes.</p>
 */
public record FlightSimulationModel(
        String designator,
        String originCode,
        String destinationCode,
        String departureIso,
        String arrivalIso,
        double loadedFuelKg,
        double tripBurnKg,
        double requiredFuelKg,
        List<ResolvedSegment> segments,
        AircraftPhysics aircraft,
        FuelBurnResult fuelBurn,
        FlightPlanTestVerdict verdict) {

    public FlightSimulationModel {
        segments = (segments == null) ? List.of() : List.copyOf(segments);
    }

    public boolean passed() {
        return verdict != null && verdict.passed();
    }
}