package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link SegmentKey}, the single shared (legNumber, segmentIndex)
 * derivation that US082 (attach weather) and US085 (resolve weather) must agree on
 * so the per-segment ordinals cannot drift (design-notes §3.1).
 */
public class SegmentKeyTest {

    @Test
    public void ensureOfBuildsKeyWithAccessors() {
        final SegmentKey k = SegmentKey.of(1, 0);
        assertEquals(1, k.legNumber());
        assertEquals(0, k.segmentIndex());
    }

    @Test
    public void ensureEqualKeysAreEqualAndShareHashCode() {
        final SegmentKey a = SegmentKey.of(2, 3);
        final SegmentKey b = SegmentKey.of(2, 3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a); // reflexive
    }

    @Test
    public void ensureKeysWithDifferentOrdinalsDiffer() {
        assertNotEquals(SegmentKey.of(1, 0), SegmentKey.of(1, 1));
        assertNotEquals(SegmentKey.of(1, 0), SegmentKey.of(2, 0));
    }

    @Test
    public void ensureNotEqualToNullOrOtherType() {
        final SegmentKey k = SegmentKey.of(1, 0);
        assertNotEquals(k, null);
        assertNotEquals(k, "1-0");
    }

    @Test
    public void ensureNegativeOrdinalsRejected() {
        try {
            SegmentKey.of(-1, 0);
            assertTrue("expected IllegalArgumentException", false);
        } catch (final IllegalArgumentException expected) {
            // ok
        }
    }
}