package lapr4.lprog.flightdsl.semantic;

import java.util.List;
import java.util.stream.Collectors;

/** Outcome of a validator pass: zero findings = valid. */
public record SemanticValidationResult(List<SemanticError> findings) {

    public boolean isValid() { return findings.isEmpty(); }

    public boolean has(SemanticErrorCode code) {
        return findings.stream().anyMatch(f -> f.code() == code);
    }

    public long count(SemanticErrorCode code) {
        return findings.stream().filter(f -> f.code() == code).count();
    }

    /** Multi-line rendering, one finding per line. */
    public String render() {
        if (findings.isEmpty()) return "OK (0 findings).";
        return findings.stream().map(SemanticError::render).collect(Collectors.joining("\n"));
    }
}