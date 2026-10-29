package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the US085 {@link FlightPlanTestResult} value object returned by the
 * {@link FlightPlanTestService} to the application/UI layer.
 */
public class FlightPlanTestResultTest {

    @Test
    public void ensureSuccessCarriesFlagAndMessage() {
        final FlightPlanTestResult r = FlightPlanTestResult.success("Plan tested OK");
        assertTrue(r.isSuccess());
        assertEquals("Plan tested OK", r.message());
        assertEquals("SUCCESS: Plan tested OK", r.toString());
    }

    @Test
    public void ensureFailureCarriesReason() {
        final FlightPlanTestResult r = FlightPlanTestResult.failure("Insufficient fuel");
        assertFalse(r.isSuccess());
        assertEquals("Insufficient fuel", r.message());
        assertEquals("FAILURE: Insufficient fuel", r.toString());
    }

    @Test
    public void ensureFailureWithNullMessageFallsBackToDefault() {
        final FlightPlanTestResult r = FlightPlanTestResult.failure(null);
        assertFalse(r.isSuccess());
        assertEquals("Test failed", r.message());
    }
}