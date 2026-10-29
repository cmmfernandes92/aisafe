package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FlightOutcomeTest {

    @Test
    public void ensureKnownLabelsMapDirectly() {
        assertEquals(FlightOutcome.COMPLETED, FlightOutcome.fromReport("completed", 0));
        assertEquals(FlightOutcome.ABORTED, FlightOutcome.fromReport("aborted", 1));
        assertEquals(FlightOutcome.TERMINATED_BY_VIOLATION, FlightOutcome.fromReport("terminated", 2));
        assertEquals(FlightOutcome.TERMINATED_BY_VIOLATION,
                FlightOutcome.fromReport("terminated_by_violation", 2));
        assertEquals(FlightOutcome.CRASHED, FlightOutcome.fromReport("crashed", 3));
    }

    @Test
    public void ensureLabelMatchingIsCaseInsensitiveAndTrimmed() {
        assertEquals(FlightOutcome.COMPLETED, FlightOutcome.fromReport("  COMPLETED ", 0));
    }

    @Test
    public void ensureUnknownLabelFallsBackOnExitCode() {
        assertEquals(FlightOutcome.COMPLETED, FlightOutcome.fromReport("?", 0));
        assertEquals(FlightOutcome.TERMINATED_BY_VIOLATION, FlightOutcome.fromReport("?", 2));
        assertEquals(FlightOutcome.CRASHED, FlightOutcome.fromReport("?", 3));
        assertEquals(FlightOutcome.ABORTED, FlightOutcome.fromReport("?", 99));
    }

    @Test
    public void ensureNullLabelFallsBackOnExitCode() {
        assertEquals(FlightOutcome.COMPLETED, FlightOutcome.fromReport(null, 0));
        assertEquals(FlightOutcome.ABORTED, FlightOutcome.fromReport(null, 1));
    }

    @Test
    public void ensureEnumValuesAreReachable() {
        assertEquals(FlightOutcome.CRASHED, FlightOutcome.valueOf("CRASHED"));
        assertEquals(4, FlightOutcome.values().length);
    }
}