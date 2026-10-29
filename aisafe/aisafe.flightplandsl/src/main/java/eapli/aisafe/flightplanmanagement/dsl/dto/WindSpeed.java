package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record WindSpeed(BigDecimal metersPerSecond, SourceLocation location) {

    public WindSpeed {
        Objects.requireNonNull(metersPerSecond);
        Objects.requireNonNull(location);
    }
}
