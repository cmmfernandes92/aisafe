package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulator shared by every semantic rule, so a single validator pass collects
 * all findings instead of stopping at the first one. Required by §3.4.5:
 * <em>"All semantic errors must be collected and reported in a single execution."</em>
 */
public final class ErrorCollector {

    private final List<SemanticError> errors = new ArrayList<>();

    public void error(SemanticErrorCode code, String message, Position position) {
        errors.add(new SemanticError(code, message, position));
    }

    public List<SemanticError> errors() {
        return Collections.unmodifiableList(errors);
    }
}