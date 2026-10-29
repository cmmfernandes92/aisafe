package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;

public class SimulationRunTest {

    private static final AreaCode LPPC = AreaCode.valueOf("LPPC");
    private static final LocalDateTime RUN_TS = LocalDateTime.parse("2026-06-12T14:00:00");

    private static FlightExecutionStatus status(final int id) {
        return new FlightExecutionStatus(id, "R" + id + "@2026-06-12", FlightOutcome.COMPLETED, 0);
    }

    private static SimulationRun run(final List<FlightExecutionStatus> statuses,
            final List<SafetyViolation> violations) {
        return new SimulationRun(LPPC, RUN_TS, "clean", statuses.size(), 60.0, "completed",
                ValidationOutcome.PASS, statuses, violations);
    }

    @Test
    public void ensureRunExposesItsAssembledState() {
        final SimulationRun r = run(List.of(status(201), status(202)), List.of());

        assertNotNull(r.identity());
        assertEquals(LPPC, r.areaCode());
        assertEquals(RUN_TS, r.runTimestamp());
        assertEquals("clean", r.scenario());
        assertEquals(2, r.totalFlights());
        assertEquals(60.0, r.secondsPerStep(), 0.0001);
        assertEquals("completed", r.endReason());
        assertEquals(ValidationOutcome.PASS, r.validationOutcome());
        assertEquals(2, r.flightStatuses().size());
        assertTrue(r.violations().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureFlightStatusesAreUnmodifiable() {
        run(List.of(status(201)), List.of()).flightStatuses().add(status(202));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureViolationsAreUnmodifiable() {
        final SafetyViolation v = SafetyViolation.altitude(1, RUN_TS,
                ViolationKind.ALTITUDE_CEILING,
                new InvolvedFlight(301, "R301@2026-06-12", 0, 0, 9000, 0, 0, 0), 10.0);
        run(List.of(status(201)), List.of()).violations().add(v);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureStatusCountMustMatchTotalFlights() {
        new SimulationRun(LPPC, RUN_TS, "clean", 3, 60.0, "completed",
                ValidationOutcome.PASS, List.of(status(201)), List.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNegativeTotalFlightsIsRejected() {
        new SimulationRun(LPPC, RUN_TS, "clean", -1, 60.0, "completed",
                ValidationOutcome.PASS, List.of(), List.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureNullAreaCodeIsRejected() {
        new SimulationRun(null, RUN_TS, "clean", 0, 60.0, "completed",
                ValidationOutcome.PASS, List.of(), List.of());
    }

    @Test
    public void ensureIdentityBasedEqualityAndToString() {
        final SimulationRun a = run(List.of(status(201)), List.of());
        final SimulationRun b = run(List.of(status(201)), List.of());

        assertEquals(a, a);
        assertTrue(a.sameAs(a));
        assertEquals(a.hashCode(), a.hashCode());
        assertNotEquals(a, b); // distinct minted ids
        assertTrue(a.toString().contains("LPPC"));
    }
}