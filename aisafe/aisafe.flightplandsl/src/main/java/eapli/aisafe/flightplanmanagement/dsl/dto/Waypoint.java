package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record Waypoint(Coordinates coordinates, SourceLocation location) {

    public Waypoint {
        Objects.requireNonNull(coordinates);
        Objects.requireNonNull(location);
    }
}
