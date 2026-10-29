package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record AirportIATACode(String code, SourceLocation location) implements AirportCode {

    public AirportIATACode {
        Objects.requireNonNull(code);
        Objects.requireNonNull(location);
    }
}
