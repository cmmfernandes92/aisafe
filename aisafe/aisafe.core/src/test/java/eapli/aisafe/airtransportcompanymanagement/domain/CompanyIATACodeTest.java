package eapli.aisafe.airtransportcompanymanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompanyIATACodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureIataCodeRejectsNull() {
        new CompanyIATACode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIataCodeRejectsEmpty() {
        new CompanyIATACode("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIataCodeRejectsSingleLetter() {
        new CompanyIATACode("T");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIataCodeRejectsThreeLetters() {
        new CompanyIATACode("TAP");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureIataCodeRejectsDigits() {
        new CompanyIATACode("T1");
    }

    @Test
    public void ensureIataCodeAcceptsTwoLetters() {
        final CompanyIATACode code = new CompanyIATACode("tp");
        assertEquals("TP", code.value());
    }

    @Test
    public void ensureIataCodeEqualsForSameValue() {
        assertTrue(new CompanyIATACode("TP").equals(new CompanyIATACode("tp")));
    }

    @Test
    public void ensureIataCodeNotEqualsForDifferentValue() {
        assertFalse(new CompanyIATACode("TP").equals(new CompanyIATACode("FR")));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final CompanyIATACode c = new CompanyIATACode("TP");
        assertTrue(c.equals(c));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(new CompanyIATACode("TP").equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(new CompanyIATACode("TP").equals("TP"));
    }

    @Test
    public void ensureCompareToReturnsNegativeForLesser() {
        assertTrue(new CompanyIATACode("FR").compareTo(new CompanyIATACode("TP")) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveForGreater() {
        assertTrue(new CompanyIATACode("TP").compareTo(new CompanyIATACode("FR")) > 0);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, new CompanyIATACode("TP").compareTo(new CompanyIATACode("TP")));
    }
}
