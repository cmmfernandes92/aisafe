package eapli.aisafe.reporting.domain;

import java.util.Locale;

/**
 * Per-flight execution outcome (US111.2). Maps the C report's {@code outcome}
 * string and {@code exit_code} (FLIGHT_EXIT_COMPLETED 0, FLIGHT_EXIT_TERMINATED 2,
 * FLIGHT_EXIT_WRITE_ERROR 3) onto a domain enum so the rest of the system never
 * parses raw simulator strings.
 */
public enum FlightOutcome {
    COMPLETED,
    ABORTED,
    TERMINATED_BY_VIOLATION,
    CRASHED;

    public static FlightOutcome fromReport(final String outcome, final int exitCode) {
        final String normalized = outcome == null ? "" : outcome.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "completed":
                return COMPLETED;
            case "aborted":
                return ABORTED;
            case "terminated":
            case "terminated_by_violation":
                return TERMINATED_BY_VIOLATION;
            case "crashed":
                return CRASHED;
            default:
                // Unknown label: fall back on the C exit code.
                switch (exitCode) {
                    case 0:
                        return COMPLETED;
                    case 2:
                        return TERMINATED_BY_VIOLATION;
                    case 3:
                        return CRASHED;
                    default:
                        return ABORTED;
                }
        }
    }
}