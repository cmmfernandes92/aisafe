package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public record ArrivalInfo(AirportCode airport, LocalDateTime when, SourceLocation location) {

    public ArrivalInfo {
        Objects.requireNonNull(airport);
        Objects.requireNonNull(when);
        Objects.requireNonNull(location);
    }
}
