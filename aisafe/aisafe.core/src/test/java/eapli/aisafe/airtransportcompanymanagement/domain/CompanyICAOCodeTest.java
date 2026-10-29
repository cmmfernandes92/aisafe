package eapli.aisafe.airtransportcompanymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompanyICAOCodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureIcaoCodeRejectsNull() {
        new CompanyICAOCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIcaoCodeRejectsSingleLetter() {
        new CompanyICAOCode("T");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIcaoCodeRejectsFourLetters() {
        new CompanyICAOCode("TAPS");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIcaoCodeRejectsDigits() {
        new CompanyICAOCode("T1P");
    }

    @Test
    public void ensureIcaoCodeAcceptsTwoLetters() {
        final CompanyICAOCode code = new CompanyICAOCode("ryr");
        assertEquals("RYR", code.value());
    }

    @Test
    public void ensureIcaoCodeAcceptsThreeLetters() {
        final CompanyICAOCode code = new CompanyICAOCode("TAP");
        assertEquals("TAP", code.value());
    }

    @Test
    public void ensureIcaoCodeEqualsForSameValue() {
        assertTrue(new CompanyICAOCode("TAP").equals(new CompanyICAOCode("tap")));
    }

    @Test
    public void ensureIcaoCodeNotEqualsForDifferentValue() {
        assertFalse(new CompanyICAOCode("TAP").equals(new CompanyICAOCode("RYR")));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final CompanyICAOCode c = new CompanyICAOCode("TAP");
        assertTrue(c.equals(c));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(new CompanyICAOCode("TAP").equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(new CompanyICAOCode("TAP").equals("TAP"));
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final CompanyICAOCode c = new CompanyICAOCode("TAP");
        assertEquals(c.hashCode(), c.hashCode());
    }

    @Test
    public void ensureHashCodeEqualForEqualCodes() {
        assertEquals(new CompanyICAOCode("TAP").hashCode(), new CompanyICAOCode("tap").hashCode());
    }

    @Test
    public void ensureCompareToReturnsNegativeForLesser() {
        assertTrue(new CompanyICAOCode("RYR").compareTo(new CompanyICAOCode("TAP")) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveForGreater() {
        assertTrue(new CompanyICAOCode("TAP").compareTo(new CompanyICAOCode("RYR")) > 0);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, new CompanyICAOCode("TAP").compareTo(new CompanyICAOCode("TAP")));
    }
}
