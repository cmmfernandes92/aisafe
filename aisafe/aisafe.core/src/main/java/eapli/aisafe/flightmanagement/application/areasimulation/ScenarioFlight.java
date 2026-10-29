package eapli.aisafe.flightmanagement.application.areasimulation;

import java.util.List;

/**
 * One flight in a native scenario. {@code entryStep} is the simulation step at
 * which the flight enters the area (staggered entry, SCOMP option (a)); 0 means it
 * is active from the start. The engine holds the flight (inactive, excluded from
 * violation detection) until that step.
 */
public record ScenarioFlight(int id, FlightProfile profile, List<ScenarioSegment> segments, int entryStep) {

    public ScenarioFlight {
        if (id <= 0) {
            throw new IllegalArgumentException("Flight id must be positive");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Flight profile must not be null");
        }
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Flight must have at least one segment");
        }
        if (segments.size() > AreaSimulationScenarioBuilder.MAX_SEGMENTS) {
            throw new IllegalArgumentException("Flight exceeds native segment limit");
        }
        if (entryStep < 0) {
            throw new IllegalArgumentException("entryStep must not be negative");
        }
        segments = List.copyOf(segments);
    }
}
