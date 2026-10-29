package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ElevationTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureElevationRejectsNaNValue() {
        new Elevation(Double.NaN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureElevationRejectsInfiniteValue() {
        new Elevation(Double.POSITIVE_INFINITY);
    }

    @Test
    public void ensureElevationAcceptsNegativeValues() {
        final Elevation belowSeaLevel = new Elevation(-3.0);

        assertNotNull(belowSeaLevel);
        assertEquals(-3.0, belowSeaLevel.value(), 0.0);
    }

    @Test
    public void ensureElevationEqualsPassesForSameValue() {
        assertTrue(new Elevation(114.0).equals(new Elevation(114.0)));
    }

    @Test
    public void ensureElevationEqualsFailsForDifferentValue() {
        assertFalse(new Elevation(114.0).equals(new Elevation(115.0)));
    }

    @Test
    public void ensureElevationEqualsSelf() {
        final Elevation elevation = new Elevation(114.0);

        assertTrue(elevation.equals(elevation));
    }

    @Test
    public void ensureElevationEqualsNullReturnsFalse() {
        assertFalse(new Elevation(114.0).equals(null));
    }

    @Test
    public void ensureElevationEqualsOtherTypeReturnsFalse() {
        assertFalse(new Elevation(114.0).equals("114.0"));
    }

    @Test
    public void ensureElevationHashCodeConsistentForEqualValues() {
        assertEquals(new Elevation(114.0).hashCode(), new Elevation(114.0).hashCode());
    }

    @Test
    public void ensureElevationToStringReturnsValue() {
        assertEquals("114.0", new Elevation(114.0).toString());
    }

    @Test
    public void ensureElevationValueOfMatchesConstructor() {
        assertEquals(new Elevation(114.0), Elevation.valueOf(114.0));
    }

    @Test
    public void ensureElevationProtectedConstructorExistsForORM() {
        assertTrue(new Elevation() instanceof Elevation);
    }
}
