package eapli.aisafe.flightmanagement.application.areasimulation;

public record ProfilePoint(double altitudeMeters, double speedKnots, double verticalRateMps) {

    public ProfilePoint {
        if (!Double.isFinite(altitudeMeters) || altitudeMeters < 0.0) {
            throw new IllegalArgumentException("Altitude must be a non-negative finite number");
        }
        if (!Double.isFinite(speedKnots) || speedKnots < 0.0) {
            throw new IllegalArgumentException("Speed must be a non-negative finite number");
        }
        if (!Double.isFinite(verticalRateMps)) {
            throw new IllegalArgumentException("Vertical rate must be finite");
        }
    }
}
