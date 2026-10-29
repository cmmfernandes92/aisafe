package eapli.aisafe.flightmanagement.application.areasimulation;

public record ScenarioSegment(
        String mode,
        double startLatitude,
        double startLongitude,
        double startAltitudeMeters,
        double endLatitude,
        double endLongitude,
        double endAltitudeMeters) {

    public ScenarioSegment {
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Segment mode must not be empty");
        }
        requireFinite(startLatitude, "startLatitude");
        requireFinite(startLongitude, "startLongitude");
        requireFinite(startAltitudeMeters, "startAltitudeMeters");
        requireFinite(endLatitude, "endLatitude");
        requireFinite(endLongitude, "endLongitude");
        requireFinite(endAltitudeMeters, "endAltitudeMeters");
        if (startAltitudeMeters < 0.0 || endAltitudeMeters < 0.0) {
            throw new IllegalArgumentException("Segment altitude must not be negative");
        }
    }

    private static void requireFinite(final double value, final String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }
}
