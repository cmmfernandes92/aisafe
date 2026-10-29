package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record Length(BigDecimal meters, SourceLocation location) {

    public Length {
        Objects.requireNonNull(meters);
        Objects.requireNonNull(location);
    }
}
