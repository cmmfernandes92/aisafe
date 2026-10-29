package eapli.aisafe.flightmanagement.domain;

import java.util.Objects;

import eapli.framework.domain.model.ValueObject;

public class SegmentKey implements ValueObject {

    private final int legNumber;
    private final int segmentIndex;

    public SegmentKey(final int legNumber, final int segmentIndex) {
        if (legNumber < 0 || segmentIndex < 0) {
            throw new IllegalArgumentException("Invalid segment key");
        }
        this.legNumber = legNumber;
        this.segmentIndex = segmentIndex;
    }

    /**
     * The single, shared way to key a flight-plan segment: {@code legNumber} is the
     * DSL leg number (1-based; the sample uses {@code leg 1}) and {@code segmentIndex}
     * is the 0-based position of the segment within that leg. Both US082 (attach
     * weather) and US085 (resolve weather) must derive the key this way so the
     * ordinals cannot drift.
     */
    public static SegmentKey of(final int legNumber, final int segmentIndex) {
        return new SegmentKey(legNumber, segmentIndex);
    }

    public int legNumber() {
        return legNumber;
    }

    public int segmentIndex() {
        return segmentIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SegmentKey)) return false;
        SegmentKey that = (SegmentKey) o;
        return legNumber == that.legNumber &&
                segmentIndex == that.segmentIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(legNumber, segmentIndex);
    }
}
