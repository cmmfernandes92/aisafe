package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.List;
import java.util.Objects;

public record FlightLeg(
        int number,
        DepartureInfo departure,
        ArrivalInfo arrival,
        FuelLoad fuel,
        List<FlightSegment> segments,
        SourceLocation location) {

    public FlightLeg {
        Objects.requireNonNull(departure);
        Objects.requireNonNull(arrival);
        Objects.requireNonNull(fuel);
        Objects.requireNonNull(segments);
        Objects.requireNonNull(location);
        segments = List.copyOf(segments);
    }
}
