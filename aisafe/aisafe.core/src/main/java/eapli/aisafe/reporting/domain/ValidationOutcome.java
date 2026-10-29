package eapli.aisafe.reporting.domain;

import java.util.Locale;

/**
 * The overall PASS/FAIL verdict of a simulation run (US111.4), wrapping the C
 * report's {@code validation=PASS|FAIL} line. PASS means stored violation events
 * stayed within the run's threshold; FAIL means it was exceeded.
 */
public enum ValidationOutcome {
    PASS,
    FAIL;

    public static ValidationOutcome fromReport(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Report is missing the validation verdict");
        }
        switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "PASS":
                return PASS;
            case "FAIL":
                return FAIL;
            default:
                throw new IllegalArgumentException("Unknown validation verdict: " + raw);
        }
    }
}