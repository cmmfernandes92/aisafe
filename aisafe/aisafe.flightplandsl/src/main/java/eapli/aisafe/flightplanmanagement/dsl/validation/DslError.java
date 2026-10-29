package eapli.aisafe.flightplanmanagement.dsl.validation;

public record DslError(Phase phase, String code, String message, int line, int column) {

    public enum Phase {
        LEXICAL,
        SYNTACTIC,
        SEMANTIC
    }
}
