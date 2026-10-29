package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record FlightDesignator(String value) {

    public FlightDesignator {
        Objects.requireNonNull(value);
    }
}
