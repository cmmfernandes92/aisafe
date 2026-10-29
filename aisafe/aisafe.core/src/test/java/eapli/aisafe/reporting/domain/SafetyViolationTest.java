package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

public class SafetyViolationTest {

    private static final LocalDateTime TS = LocalDateTime.parse("2026-06-12T14:05:00");

    private static InvolvedFlight flight(final int id) {
        return new InvolvedFlight(id, "R" + id + "@2026-06-12",
                1000.0 + id, 2000.0 + id, 9000.0, 200.0, 0.0, 0.0);
    }

    // --- separation ----------------------------------------------------------

    @Test
    public void ensureSeparationCarriesTwoFlightsKindAndDistances() {
        final SafetyViolation v = SafetyViolation.separation(5, TS, flight(201), flight(202), 3.5, 120.0);

        assertEquals(5, v.step());
        assertEquals(TS, v.timestamp());
        assertEquals(ViolationKind.SEPARATION, v.kind());
        assertEquals(3.5, v.horizontalDistance(), 0.0001);
        assertEquals(120.0, v.verticalDistance(), 0.0001);
        assertEquals(2, v.involvedFlights().size());
        assertEquals(2, v.positions().size());
        assertEquals(flight(201).position(), v.positions().get(0));
    }

    @Test
    public void ensureToStringMentionsStepAndKind() {
        final SafetyViolation v = SafetyViolation.separation(5, TS, flight(201), flight(202), 3.5, 120.0);

        final String text = v.toString();
        assertTrue(text.contains("step 5"));
        assertTrue(text.contains("SEPARATION"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSeparationRejectsNegativeStep() {
        SafetyViolation.separation(-1, TS, flight(201), flight(202), 3.5, 120.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSeparationRejectsTwoCopiesOfTheSameFlight() {
        SafetyViolation.separation(5, TS, flight(201), flight(201), 3.5, 120.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSeparationRejectsNullTimestamp() {
        SafetyViolation.separation(5, null, flight(201), flight(202), 3.5, 120.0);
    }

    // --- altitude ------------------------------------------------------------

    @Test
    public void ensureAltitudeCarriesOneFlightAndMetresPastBound() {
        final SafetyViolation v = SafetyViolation.altitude(8, TS, ViolationKind.ALTITUDE_CEILING,
                flight(301), 250.0);

        assertEquals(8, v.step());
        assertEquals(ViolationKind.ALTITUDE_CEILING, v.kind());
        assertEquals(0.0, v.horizontalDistance(), 0.0001);
        assertEquals(250.0, v.verticalDistance(), 0.0001);
        assertEquals(1, v.involvedFlights().size());
        assertEquals(1, v.positions().size());
        assertEquals(flight(301).position(), v.positions().get(0));
    }

    @Test
    public void ensureAltitudeFloorIsAcceptedToo() {
        final SafetyViolation v = SafetyViolation.altitude(3, TS, ViolationKind.ALTITUDE_FLOOR,
                flight(301), 40.0);

        assertEquals(ViolationKind.ALTITUDE_FLOOR, v.kind());
        assertEquals(1, v.involvedFlights().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAltitudeRejectsASeparationKind() {
        SafetyViolation.altitude(8, TS, ViolationKind.SEPARATION, flight(301), 250.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAltitudeRejectsNegativeStep() {
        SafetyViolation.altitude(-1, TS, ViolationKind.ALTITUDE_CEILING, flight(301), 250.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAltitudeRejectsNullFlight() {
        SafetyViolation.altitude(8, TS, ViolationKind.ALTITUDE_CEILING, null, 250.0);
    }
}