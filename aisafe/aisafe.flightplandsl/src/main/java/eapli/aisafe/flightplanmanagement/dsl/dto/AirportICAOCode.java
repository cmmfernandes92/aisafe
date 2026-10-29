package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.util.Objects;

public record AirportICAOCode(String code, SourceLocation location) implements AirportCode {

    public AirportICAOCode {
        Objects.requireNonNull(code);
        Objects.requireNonNull(location);
    }
}
