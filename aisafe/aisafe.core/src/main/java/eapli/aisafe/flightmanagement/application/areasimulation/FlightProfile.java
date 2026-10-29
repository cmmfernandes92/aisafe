package eapli.aisafe.flightmanagement.application.areasimulation;

import java.util.List;

public record FlightProfile(List<ProfilePoint> climb, List<ProfilePoint> descend, double cruiseSpeedKnots) {

    public FlightProfile {
        if (climb == null || climb.isEmpty()) {
            throw new IllegalArgumentException("Climb profile must not be empty");
        }
        if (descend == null || descend.isEmpty()) {
            throw new IllegalArgumentException("Descend profile must not be empty");
        }
        if (!Double.isFinite(cruiseSpeedKnots) || cruiseSpeedKnots <= 0.0) {
            throw new IllegalArgumentException("Cruise speed must be positive");
        }
        climb = List.copyOf(climb);
        descend = List.copyOf(descend);
    }

    public static FlightProfile defaultJet() {
        return new FlightProfile(
                List.of(
                        new ProfilePoint(0.0, 210.0, 12.0),
                        new ProfilePoint(5000.0, 260.0, 9.5),
                        new ProfilePoint(9000.0, 290.0, 7.0)),
                List.of(
                        new ProfilePoint(9000.0, 300.0, -10.0),
                        new ProfilePoint(3000.0, 250.0, -8.0),
                        new ProfilePoint(0.0, 140.0, -5.0)),
                460.0);
    }
}
