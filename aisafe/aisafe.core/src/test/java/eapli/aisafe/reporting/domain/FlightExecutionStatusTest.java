package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlightExecutionStatusTest {

    private static final String REF = "TP123@2026-06-12";

    @Test
    public void ensureStatusExposesItsFields() {
        final FlightExecutionStatus status =
                new FlightExecutionStatus(201, REF, FlightOutcome.COMPLETED, 0);

        assertEquals(201, status.surrogateId());
        assertEquals(REF, status.flightRef());
        assertEquals(FlightOutcome.COMPLETED, status.outcome());
        assertEquals(0, status.exitCode());
    }

    @Test
    public void ensureToStringDescribesRefOutcomeAndExitCode() {
        final FlightExecutionStatus status =
                new FlightExecutionStatus(202, REF, FlightOutcome.TERMINATED_BY_VIOLATION, 2);

        final String text = status.toString();
        assertTrue(text.contains(REF));
        assertTrue(text.contains("TERMINATED_BY_VIOLATION"));
        assertTrue(text.contains("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullOutcomeIsRejected() {
        new FlightExecutionStatus(201, REF, null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEmptyFlightRefIsRejected() {
        new FlightExecutionStatus(201, "", FlightOutcome.COMPLETED, 0);
    }
}