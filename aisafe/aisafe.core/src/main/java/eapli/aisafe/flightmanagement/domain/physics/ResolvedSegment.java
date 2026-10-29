package eapli.aisafe.flightmanagement.domain.physics;

/**
 * A flight-plan segment after US085 wind resolution: the DSL geometry and cruise
 * altitude, plus the <em>effective</em> wind for this segment (the pilot's
 * attached forecast where present, otherwise the DSL baseline) and where that
 * wind came from. This is what the fuel-burn model integrates over and what the
 * simulator view renders per segment.
 */
public record ResolvedSegment(
        int legNumber,
        int segmentIndex,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        double altitudeMeters,
        double windDirectionDeg,
        double windSpeedMps,
        WindSource windSource) {
}