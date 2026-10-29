package eapli.aisafe.flightplanmanagement.dsl.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record FuelLoad(BigDecimal quantity, FuelUnit unit, SourceLocation location) {

    public FuelLoad {
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(unit);
        Objects.requireNonNull(location);
    }
}
