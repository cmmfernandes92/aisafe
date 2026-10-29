package eapli.aisafe.flightmanagement.application.areasimulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eapli.aisafe.flightmanagement.domain.FlightId;

public record AreaSimulationScenario(
        SimParams params,
        List<ScenarioFlight> flights,
        Map<Integer, FlightId> idMap) {

    public AreaSimulationScenario {
        if (params == null) {
            throw new IllegalArgumentException("Simulation parameters must not be null");
        }
        if (flights == null || flights.isEmpty()) {
            throw new IllegalArgumentException("Scenario must contain at least one flight");
        }
        if (flights.size() > AreaSimulationScenarioBuilder.MAX_FLIGHTS) {
            throw new IllegalArgumentException("Scenario exceeds native flight limit");
        }
        flights = List.copyOf(flights);
        idMap = idMap == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(idMap));
    }
}
