package eapli.aisafe.flightmanagement.application.parsing;

/**
 * A single problem found while compiling a flight plan's DSL, in a form the core
 * understands without depending on the DSL module. It mirrors the compiler's
 * error shape so US085 can report errors "clearly with type and location"
 * (US085.4): {@code phase} is the analysis stage (LEXICAL/SYNTACTIC/SEMANTIC),
 * and {@code line}/{@code column} locate it in the source.
 *
 * <p>The DSL-backed {@code FlightPlanCompiler} adapter maps the compiler's native
 * error type onto this record; core never sees the DSL types.</p>
 */
public record FlightPlanIssue(String phase, String code, String message, int line, int column) {

    public FlightPlanIssue {
        if (phase == null || phase.isBlank()) {
            throw new IllegalArgumentException("Issue phase must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Issue message must not be empty");
        }
    }

    /** A human-readable {@code PHASE [line:col] message} rendering for reports. */
    public String describe() {
        final String where = (line >= 0 && column >= 0) ? " [" + line + ":" + column + "]" : "";
        return phase + where + " " + message;
    }
}