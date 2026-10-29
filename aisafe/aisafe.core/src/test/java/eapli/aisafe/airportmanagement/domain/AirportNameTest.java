package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirportNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportNameRejectsNullValue() {
        new AirportName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportNameRejectsEmptyValue() {
        new AirportName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportNameRejectsBlankValue() {
        new AirportName("   ");
    }

    @Test
    public void ensureAirportNameAcceptsValidValue() {
        assertEquals("Humberto Delgado", new AirportName(" Humberto Delgado ").value());
    }

    @Test
    public void ensureAirportNameEqualsPassesForSameValue() {
        assertTrue(new AirportName("Porto").equals(new AirportName("Porto")));
    }

    @Test
    public void ensureAirportNameEqualsFailsForDifferentValue() {
        assertFalse(new AirportName("Porto").equals(new AirportName("Lisbon")));
    }

    @Test
    public void ensureAirportNameEqualsSelf() {
        final AirportName name = new AirportName("Lisbon");

        assertTrue(name.equals(name));
    }

    @Test
    public void ensureAirportNameEqualsNullReturnsFalse() {
        assertFalse(new AirportName("Lisbon").equals(null));
    }

    @Test
    public void ensureAirportNameEqualsOtherTypeReturnsFalse() {
        assertFalse(new AirportName("Lisbon").equals("Lisbon"));
    }

    @Test
    public void ensureAirportNameHashCodeConsistentForEqualNames() {
        assertEquals(new AirportName("Lisbon").hashCode(), new AirportName("Lisbon").hashCode());
    }

    @Test
    public void ensureAirportNameToStringReturnsValue() {
        assertEquals("Lisbon", new AirportName("Lisbon").toString());
    }

    @Test
    public void ensureAirportNameValueOfMatchesConstructor() {
        assertEquals(new AirportName("Lisbon"), AirportName.valueOf("Lisbon"));
    }

    @Test
    public void ensureAirportNameCompareToOrdersLexicographically() {
        assertTrue(new AirportName("Lisbon").compareTo(new AirportName("Porto")) < 0);
    }

    @Test
    public void ensureAirportNameProtectedConstructorExistsForORM() {
        assertTrue(new AirportName() instanceof AirportName);
    }
}
