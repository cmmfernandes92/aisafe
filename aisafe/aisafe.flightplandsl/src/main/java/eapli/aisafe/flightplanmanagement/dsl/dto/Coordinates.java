package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record Coordinates(double latitude, double longitude, SourceLocation location) {

    public Coordinates {
        Objects.requireNonNull(location);
    }
}
