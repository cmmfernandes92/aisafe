package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TownTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureTownRejectsNullValue() {
        new Town(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTownRejectsEmptyValue() {
        new Town("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTownRejectsBlankValue() {
        new Town("   ");
    }

    @Test
    public void ensureTownAcceptsValidValue() {
        assertEquals("Lisbon", new Town(" Lisbon ").value());
    }

    @Test
    public void ensureTownEqualsPassesForSameValue() {
        assertTrue(new Town("Lisbon").equals(new Town("Lisbon")));
    }

    @Test
    public void ensureTownEqualsFailsForDifferentValue() {
        assertFalse(new Town("Lisbon").equals(new Town("Porto")));
    }

    @Test
    public void ensureTownEqualsSelf() {
        final Town town = new Town("Lisbon");

        assertTrue(town.equals(town));
    }

    @Test
    public void ensureTownEqualsNullReturnsFalse() {
        assertFalse(new Town("Lisbon").equals(null));
    }

    @Test
    public void ensureTownEqualsOtherTypeReturnsFalse() {
        assertFalse(new Town("Lisbon").equals("Lisbon"));
    }

    @Test
    public void ensureTownHashCodeConsistentForEqualTowns() {
        assertEquals(new Town("Lisbon").hashCode(), new Town("Lisbon").hashCode());
    }

    @Test
    public void ensureTownToStringReturnsValue() {
        assertEquals("Lisbon", new Town("Lisbon").toString());
    }

    @Test
    public void ensureTownValueOfMatchesConstructor() {
        assertEquals(new Town("Lisbon"), Town.valueOf("Lisbon"));
    }

    @Test
    public void ensureTownCompareToOrdersLexicographically() {
        assertTrue(new Town("Lisbon").compareTo(new Town("Porto")) < 0);
    }

    @Test
    public void ensureTownProtectedConstructorExistsForORM() {
        assertTrue(new Town() instanceof Town);
    }
}
