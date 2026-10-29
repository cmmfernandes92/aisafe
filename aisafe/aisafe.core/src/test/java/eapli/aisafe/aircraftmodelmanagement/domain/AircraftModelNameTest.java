package eapli.aisafe.aircraftmodelmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AircraftModelNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelNameRejectsNull() {
        new AircraftModelName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelNameRejectsEmpty() {
        new AircraftModelName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAircraftModelNameRejectsBlank() {
        new AircraftModelName("   ");
    }

    @Test
    public void ensureAircraftModelNameAcceptsValidValue() {
        final AircraftModelName name = new AircraftModelName("A320neo");
        assertEquals("A320neo", name.value());
    }

    @Test
    public void ensureAircraftModelNameStripsWhitespace() {
        final AircraftModelName name = new AircraftModelName("  B737 MAX  ");
        assertEquals("B737 MAX", name.value());
    }

    @Test
    public void ensureAircraftModelNameEqualsForSameValue() {
        assertTrue(new AircraftModelName("A320neo").equals(new AircraftModelName("A320neo")));
    }

    @Test
    public void ensureAircraftModelNameNotEqualsForDifferentValue() {
        assertFalse(new AircraftModelName("A320neo").equals(new AircraftModelName("B737 MAX")));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final AircraftModelName n = new AircraftModelName("A320neo");
        assertTrue(n.equals(n));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(new AircraftModelName("A320neo").equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(new AircraftModelName("A320neo").equals("A320neo"));
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final AircraftModelName n = new AircraftModelName("A320neo");
        assertEquals(n.hashCode(), n.hashCode());
    }

    @Test
    public void ensureHashCodeEqualForEqualNames() {
        assertEquals(new AircraftModelName("A320neo").hashCode(), new AircraftModelName("A320neo").hashCode());
    }

    @Test
    public void ensureCompareToReturnsNegativeForLesser() {
        assertTrue(new AircraftModelName("A320neo").compareTo(new AircraftModelName("B737")) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveForGreater() {
        assertTrue(new AircraftModelName("B737").compareTo(new AircraftModelName("A320neo")) > 0);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, new AircraftModelName("A320neo").compareTo(new AircraftModelName("A320neo")));
    }
}
