package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.List;
import java.util.Objects;

public record FlightPlanFile(List<FlightPlanDTO> flightPlans) {

    public FlightPlanFile {
        Objects.requireNonNull(flightPlans);
        flightPlans = List.copyOf(flightPlans);
    }
}
