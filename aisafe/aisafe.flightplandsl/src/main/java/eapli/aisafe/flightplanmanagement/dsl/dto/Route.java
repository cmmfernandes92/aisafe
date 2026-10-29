package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record Route(AirportCode origin, AirportCode destination, SourceLocation location) {

    public Route {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(location);
    }
}
