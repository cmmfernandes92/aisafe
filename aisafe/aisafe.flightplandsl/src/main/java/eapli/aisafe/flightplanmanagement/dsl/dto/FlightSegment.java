package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.List;
import java.util.Objects;

public record FlightSegment(
        Waypoint start,
        Waypoint end,
        List<AltitudeSlot> slots,
        WindDirection windDirection,
        WindSpeed windSpeed,
        SourceLocation location) {

    public FlightSegment {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(slots);
        Objects.requireNonNull(windDirection);
        Objects.requireNonNull(windSpeed);
        Objects.requireNonNull(location);
        slots = List.copyOf(slots);
    }
}
