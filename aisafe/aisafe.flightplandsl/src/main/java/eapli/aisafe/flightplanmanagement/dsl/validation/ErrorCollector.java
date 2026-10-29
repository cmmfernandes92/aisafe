package eapli.aisafe.flightplanmanagement.dsl.validation;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {

    private final List<DslError> errors = new ArrayList<>();

    public void add(final DslError error) {
        errors.add(error);
    }

    public List<DslError> all() {
        return List.copyOf(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean has(final DslError.Phase phase) {
        return errors.stream().anyMatch(error -> error.phase() == phase);
    }
}
