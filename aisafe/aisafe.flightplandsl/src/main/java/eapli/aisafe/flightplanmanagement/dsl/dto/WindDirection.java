package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record WindDirection(BigDecimal degrees, SourceLocation location) {

    public WindDirection {
        Objects.requireNonNull(degrees);
        Objects.requireNonNull(location);
    }
}
