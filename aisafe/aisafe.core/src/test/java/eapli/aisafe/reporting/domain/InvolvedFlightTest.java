package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InvolvedFlightTest {

    private static InvolvedFlight sample() {
        return new InvolvedFlight(201, "TP123@2026-06-12",
                1000.0, 2000.0, 9000.0, 200.0, -10.0, 5.0);
    }

    @Test
    public void ensureInvolvedFlightExposesItsFields() {
        final InvolvedFlight f = sample();

        assertEquals(201, f.surrogateId());
        assertEquals("TP123@2026-06-12", f.flightRef());
        assertEquals(1000.0, f.x(), 0.0001);
        assertEquals(2000.0, f.y(), 0.0001);
        assertEquals(9000.0, f.alt(), 0.0001);
        assertEquals(200.0, f.vx(), 0.0001);
        assertEquals(-10.0, f.vy(), 0.0001);
        assertEquals(5.0, f.vz(), 0.0001);
    }

    @Test
    public void ensurePositionIsDerivedFromCoordinatesAndAltitude() {
        final Position p = sample().position();

        assertEquals(new Position(1000.0, 2000.0, 9000.0), p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEmptyFlightRefIsRejected() {
        new InvolvedFlight(201, "", 0, 0, 0, 0, 0, 0);
    }
}