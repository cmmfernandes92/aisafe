package eapli.aisafe.flightmanagement.application.areasimulation;

public record SimParams(
        String name,
        int totalSteps,
        double secondsPerStep,
        int historyDepth,
        int maxViolations,
        double hMin,
        double vMin,
        double areaXMin,
        double areaXMax,
        double areaYMin,
        double areaYMax,
        double areaAltMin,
        double areaAltMax,
        double refLat,
        double refLon) {

    public SimParams {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Scenario name must not be empty");
        }
        if (totalSteps <= 0) {
            throw new IllegalArgumentException("Total steps must be positive");
        }
        if (!Double.isFinite(secondsPerStep) || secondsPerStep <= 0.0) {
            throw new IllegalArgumentException("Seconds per step must be positive");
        }
        if (historyDepth < 2 || historyDepth > 8) {
            throw new IllegalArgumentException("History depth must be between 2 and 8");
        }
        if (maxViolations < 0) {
            throw new IllegalArgumentException("Max violations must not be negative");
        }
        if (!Double.isFinite(hMin) || hMin <= 0.0 || !Double.isFinite(vMin) || vMin <= 0.0) {
            throw new IllegalArgumentException("Safety thresholds must be positive");
        }
        if (areaXMin >= areaXMax || areaYMin >= areaYMax || areaAltMin >= areaAltMax) {
            throw new IllegalArgumentException("Simulation area bounds are invalid");
        }
        requireFinite(refLat, "refLat");
        requireFinite(refLon, "refLon");
    }

    private static void requireFinite(final double value, final String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }
}
