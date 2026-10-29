package eapli.aisafe.flightmanagement.application.areasimulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.framework.validations.Preconditions;

public class SurrogateIdMap {

    private final Map<FlightId, Integer> byFlight = new LinkedHashMap<>();
    private final Map<Integer, FlightId> byId = new LinkedHashMap<>();
    private int nextId = 1;

    public int idFor(final FlightId flightId) {
        Preconditions.nonNull(flightId);
        return byFlight.computeIfAbsent(flightId, key -> {
            final int id = nextId++;
            byId.put(id, key);
            return id;
        });
    }

    public Map<Integer, FlightId> asMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(byId));
    }
}
