package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirportICAOCodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportICAOCodeRejectsNullValue() {
        new AirportICAOCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportICAOCodeRejectsWrongLength() {
        new AirportICAOCode("LPP");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportICAOCodeRejectsNonAlphaCharacters() {
        new AirportICAOCode("LP1T");
    }

    @Test
    public void ensureAirportICAOCodeNormalizesLowercaseValue() {
        assertEquals("LPPT", new AirportICAOCode("lppt").value());
    }

    @Test
    public void ensureAirportICAOCodeEqualsPassesForSameValue() {
        assertTrue(new AirportICAOCode("LPPT").equals(new AirportICAOCode("LPPT")));
    }

    @Test
    public void ensureAirportICAOCodeEqualsFailsForDifferentValue() {
        assertFalse(new AirportICAOCode("LPPT").equals(new AirportICAOCode("LPPR")));
    }

    @Test
    public void ensureAirportICAOCodeValueOfMatchesConstructor() {
        assertEquals(new AirportICAOCode("LPPT"), AirportICAOCode.valueOf("LPPT"));
    }

    @Test
    public void ensureAirportICAOCodeCompareToOrdersLexicographically() {
        assertTrue(new AirportICAOCode("LPPR").compareTo(new AirportICAOCode("LPPT")) < 0);
    }

    @Test
    public void ensureAirportICAOCodeEqualsSelf() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");

        assertTrue(code.equals(code));
    }

    @Test
    public void ensureAirportICAOCodeEqualsNullReturnsFalse() {
        assertFalse(new AirportICAOCode("LPPT").equals(null));
    }

    @Test
    public void ensureAirportICAOCodeEqualsOtherTypeReturnsFalse() {
        assertFalse(new AirportICAOCode("LPPT").equals("LPPT"));
    }

    @Test
    public void ensureAirportICAOCodeHashCodeConsistentForEqualCodes() {
        assertEquals(new AirportICAOCode("LPPT").hashCode(), new AirportICAOCode("LPPT").hashCode());
    }

    @Test
    public void ensureAirportICAOCodeToStringReturnsValue() {
        assertEquals("LPPT", new AirportICAOCode("LPPT").toString());
    }

    @Test
    public void ensureAirportICAOCodeProtectedConstructorExistsForORM() {
        assertTrue(new AirportICAOCode() instanceof AirportICAOCode);
    }
}
