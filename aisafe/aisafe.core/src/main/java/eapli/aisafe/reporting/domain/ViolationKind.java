package eapli.aisafe.reporting.domain;

import java.util.Locale;

/**
 * The nature of a safety violation (US111.3). A {@code [violations]} row may be a
 * loss of separation between two aircraft, or a single aircraft busting its
 * altitude ceiling or floor. Maps the C report's raw {@code kind} string onto a
 * domain enum so the rest of the system never parses raw simulator strings.
 */
public enum ViolationKind {
    SEPARATION,
    ALTITUDE_CEILING,
    ALTITUDE_FLOOR,
    UNKNOWN;

    public static ViolationKind fromReport(final String raw) {
        final String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "":
            case "separation":
                return SEPARATION;
            case "altitude_ceiling":
                return ALTITUDE_CEILING;
            case "altitude_floor":
                return ALTITUDE_FLOOR;
            default:
                return UNKNOWN;
        }
    }

    /** Whether this kind is an altitude bust (ceiling or floor) rather than a separation. */
    public boolean isAltitude() {
        return this == ALTITUDE_CEILING || this == ALTITUDE_FLOOR;
    }
}
