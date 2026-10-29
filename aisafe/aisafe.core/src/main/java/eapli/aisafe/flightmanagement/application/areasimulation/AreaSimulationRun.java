package eapli.aisafe.flightmanagement.application.areasimulation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import eapli.aisafe.flightmanagement.domain.FlightId;

public record AreaSimulationRun(
        Path runDir,
        Path reportPath,
        Path violationsPath,
        int exitCode,
        Map<Integer, FlightId> idMap) {

    public AreaSimulationRun {
        if (runDir == null || reportPath == null || violationsPath == null) {
            throw new IllegalArgumentException("Run paths must not be null");
        }
        idMap = idMap == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(idMap));
    }
}
