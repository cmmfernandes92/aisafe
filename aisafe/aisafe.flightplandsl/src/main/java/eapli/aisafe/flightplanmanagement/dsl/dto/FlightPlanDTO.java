package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.List;
import java.util.Objects;

public record FlightPlanDTO(
        FlightDesignator designator,
        FlightType type,
        Route route,
        List<FlightLeg> legs,
        SourceLocation location) {

    public FlightPlanDTO {
        Objects.requireNonNull(designator);
        Objects.requireNonNull(type);
        Objects.requireNonNull(route);
        Objects.requireNonNull(legs);
        Objects.requireNonNull(location);
        legs = List.copyOf(legs);
    }
}
