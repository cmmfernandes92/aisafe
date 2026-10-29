package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record AltitudeSlot(Length altitude, Length width, SourceLocation location) {

    public AltitudeSlot {
        Objects.requireNonNull(altitude);
        Objects.requireNonNull(width);
        Objects.requireNonNull(location);
    }
}
