package eapli.aisafe.airportmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirportIATACodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportIATACodeRejectsNullValue() {
        new AirportIATACode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportIATACodeRejectsWrongLength() {
        new AirportIATACode("LISA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAirportIATACodeRejectsNonAlphaCharacters() {
        new AirportIATACode("L1S");
    }

    @Test
    public void ensureAirportIATACodeNormalizesLowercaseValue() {
        assertEquals("LIS", new AirportIATACode("lis").value());
    }

    @Test
    public void ensureAirportIATACodeEqualsPassesForSameValue() {
        assertTrue(new AirportIATACode("LIS").equals(new AirportIATACode("LIS")));
    }

    @Test
    public void ensureAirportIATACodeEqualsFailsForDifferentValue() {
        assertFalse(new AirportIATACode("LIS").equals(new AirportIATACode("OPO")));
    }

    @Test
    public void ensureAirportIATACodeValueOfMatchesConstructor() {
        assertEquals(new AirportIATACode("LIS"), AirportIATACode.valueOf("LIS"));
    }

    @Test
    public void ensureAirportIATACodeEqualsSelf() {
        final AirportIATACode code = new AirportIATACode("LIS");

        assertTrue(code.equals(code));
    }

    @Test
    public void ensureAirportIATACodeEqualsNullReturnsFalse() {
        assertFalse(new AirportIATACode("LIS").equals(null));
    }

    @Test
    public void ensureAirportIATACodeEqualsOtherTypeReturnsFalse() {
        assertFalse(new AirportIATACode("LIS").equals("LIS"));
    }

    @Test
    public void ensureAirportIATACodeHashCodeConsistentForEqualCodes() {
        assertEquals(new AirportIATACode("LIS").hashCode(), new AirportIATACode("LIS").hashCode());
    }

    @Test
    public void ensureAirportIATACodeToStringReturnsValue() {
        assertEquals("LIS", new AirportIATACode("LIS").toString());
    }

    @Test
    public void ensureAirportIATACodeCompareToOrdersLexicographically() {
        assertTrue(new AirportIATACode("LIS").compareTo(new AirportIATACode("OPO")) < 0);
    }

    @Test
    public void ensureAirportIATACodeProtectedConstructorExistsForORM() {
        assertTrue(new AirportIATACode() instanceof AirportIATACode);
    }
}
