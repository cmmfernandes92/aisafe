package eapli.aisafe.countrymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountryCodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryCodeRejectsNullValue() {
        new CountryCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryCodeRejectsBlankValue() {
        new CountryCode("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryCodeRejectsNonNumericCharacters() {
        new CountryCode("PT");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCountryCodeRejectsMoreThanThreeDigits() {
        new CountryCode("1234");
    }

    @Test
    public void ensureCountryCodeAcceptsValidValue() {
        assertEquals("351", new CountryCode(" 351 ").value());
    }

    @Test
    public void ensureCountryCodeEqualsPassesForSameValue() {
        assertTrue(new CountryCode("351").equals(new CountryCode("351")));
    }

    @Test
    public void ensureCountryCodeEqualsFailsForDifferentValue() {
        assertFalse(new CountryCode("351").equals(new CountryCode("34")));
    }

    @Test
    public void ensureCountryCodeRejectsEmptyValue() {
        try {
            new CountryCode("");
        } catch (final IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    @Test
    public void ensureCountryCodeEqualsSelf() {
        final CountryCode code = new CountryCode("351");

        assertTrue(code.equals(code));
    }

    @Test
    public void ensureCountryCodeEqualsNullReturnsFalse() {
        assertFalse(new CountryCode("351").equals(null));
    }

    @Test
    public void ensureCountryCodeEqualsOtherTypeReturnsFalse() {
        assertFalse(new CountryCode("351").equals("351"));
    }

    @Test
    public void ensureCountryCodeHashCodeConsistentForEqualCodes() {
        assertEquals(new CountryCode("351").hashCode(), new CountryCode("351").hashCode());
    }

    @Test
    public void ensureCountryCodeToStringReturnsValue() {
        assertEquals("351", new CountryCode("351").toString());
    }

    @Test
    public void ensureCountryCodeValueOfMatchesConstructor() {
        assertEquals(new CountryCode("351"), CountryCode.valueOf("351"));
    }

    @Test
    public void ensureCountryCodeCompareToOrdersLexicographically() {
        assertTrue(new CountryCode("34").compareTo(new CountryCode("351")) < 0);
    }

    @Test
    public void ensureCountryCodeProtectedConstructorExistsForORM() {
        assertTrue(new CountryCode() instanceof CountryCode);
    }
}
