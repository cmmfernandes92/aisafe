package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ValidationOutcomeTest {

    @Test
    public void ensurePassIsParsedFromReport() {
        assertEquals(ValidationOutcome.PASS, ValidationOutcome.fromReport("PASS"));
    }

    @Test
    public void ensureFailIsParsedFromReport() {
        assertEquals(ValidationOutcome.FAIL, ValidationOutcome.fromReport("FAIL"));
    }

    @Test
    public void ensureVerdictIsCaseInsensitiveAndTrimmed() {
        assertEquals(ValidationOutcome.PASS, ValidationOutcome.fromReport("  pass "));
        assertEquals(ValidationOutcome.FAIL, ValidationOutcome.fromReport("Fail"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullVerdictIsRejected() {
        ValidationOutcome.fromReport(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureBlankVerdictIsRejected() {
        ValidationOutcome.fromReport("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureUnknownVerdictIsRejected() {
        ValidationOutcome.fromReport("MAYBE");
    }

    @Test
    public void ensureEnumValuesAreReachable() {
        assertEquals(ValidationOutcome.PASS, ValidationOutcome.valueOf("PASS"));
        assertEquals(2, ValidationOutcome.values().length);
    }
}