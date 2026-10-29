package eapli.aisafe.flightmanagement.application.parsing;

/**
 * A flight-plan segment recovered from the DSL, as plain values (no DSL types).
 * It carries the geometry, the cruise altitude, and the DSL's <em>baseline</em>
 * wind — the fallback the US085 wind resolution uses where the pilot has not
 * attached a forecast for this segment.
 *
 * <p>{@code legNumber} is the DSL leg number (1-based; the sample uses
 * {@code leg 1}) and {@code segmentIndex} is the 0-based position of the segment
 * within that leg's {@code segments} block. This ordinal pair is the shared
 * {@code SegmentKey} convention that US082 (attach) and US085 (resolve) must
 * agree on — see design-notes §3.</p>
 */
public record ParsedSegment(
        int legNumber,
        int segmentIndex,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude,
        double altitudeMeters,
        double baselineWindDirectionDeg,
        double baselineWindSpeedMps) {

    public ParsedSegment {
        if (legNumber < 0 || segmentIndex < 0) {
            throw new IllegalArgumentException("Segment ordinal must be non-negative");
        }
        if (altitudeMeters <= 0) {
            throw new IllegalArgumentException("Segment altitude must be positive");
        }
    }
}