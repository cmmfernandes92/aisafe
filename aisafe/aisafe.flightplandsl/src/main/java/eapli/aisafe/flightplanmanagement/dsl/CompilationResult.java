package eapli.aisafe.flightplanmanagement.dsl;

import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompilationResult(Optional<FlightPlanFile> file, List<DslError> errors) {

    public CompilationResult {
        Objects.requireNonNull(file);
        Objects.requireNonNull(errors);
        errors = List.copyOf(errors);
    }

    public boolean ok() {
        return errors.isEmpty();
    }
}
