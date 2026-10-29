package eapli.aisafe.weather.application.importing;

/**
 * A single skipped row in a bulk import: the source line number and the reason
 * it was rejected. Reported back to the operator in the {@link ImportReport}.
 */
public final class RowError {

    private final int line;
    private final String reason;

    public RowError(final int line, final String reason) {
        this.line = line;
        this.reason = reason;
    }

    public int line() {
        return this.line;
    }

    public String reason() {
        return this.reason;
    }

    @Override
    public String toString() {
        return "line " + line + ": " + reason;
    }
}