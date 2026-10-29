package eapli.aisafe.countrymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountryNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryNameRejectsNullValue() {
        new CountryName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryNameRejectsEmptyValue() {
        new CountryName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryNameRejectsBlankValue() {
        new CountryName("   ");
    }

    @Test
    public void ensureCountryNameAcceptsValidValue() {
        assertEquals("Portugal", new CountryName(" Portugal ").value());
    }

    @Test
    public void ensureCountryNameEqualsPassesForSameValue() {
        assertTrue(new CountryName("Portugal").equals(new CountryName("Portugal")));
    }

    @Test
    public void ensureCountryNameEqualsFailsForDifferentValue() {
        assertFalse(new CountryName("Portugal").equals(new CountryName("Spain")));
    }

    @Test
    public void ensureCountryNameEqualsSelf() {
        final CountryName name = new CountryName("Portugal");

        assertTrue(name.equals(name));
    }

    @Test
    public void ensureCountryNameEqualsNullReturnsFalse() {
        assertFalse(new CountryName("Portugal").equals(null));
    }

    @Test
    public void ensureCountryNameEqualsOtherTypeReturnsFalse() {
        assertFalse(new CountryName("Portugal").equals("Portugal"));
    }

    @Test
    public void ensureCountryNameHashCodeConsistentForEqualNames() {
        assertEquals(new CountryName("Portugal").hashCode(), new CountryName("Portugal").hashCode());
    }

    @Test
    public void ensureCountryNameToStringReturnsValue() {
        assertEquals("Portugal", new CountryName("Portugal").toString());
    }

    @Test
    public void ensureCountryNameValueOfMatchesConstructor() {
        assertEquals(new CountryName("Portugal"), CountryName.valueOf("Portugal"));
    }

    @Test
    public void ensureCountryNameCompareToOrdersLexicographically() {
        assertTrue(new CountryName("Portugal").compareTo(new CountryName("Spain")) < 0);
    }

    @Test
    public void ensureCountryNameProtectedConstructorExistsForORM() {
        assertTrue(new CountryName() instanceof CountryName);
    }
}
