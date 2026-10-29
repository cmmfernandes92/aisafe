package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public record DepartureInfo(AirportCode airport, LocalDateTime when, SourceLocation location) {

    public DepartureInfo {
        Objects.requireNonNull(airport);
        Objects.requireNonNull(when);
        Objects.requireNonNull(location);
    }
}
