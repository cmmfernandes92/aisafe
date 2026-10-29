package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViolationKindTest {

    @Test
    public void ensureSeparationIsTheDefaultForBlankAndEmptyKinds() {
        assertEquals(ViolationKind.SEPARATION, ViolationKind.fromReport(""));
        assertEquals(ViolationKind.SEPARATION, ViolationKind.fromReport("   "));
        assertEquals(ViolationKind.SEPARATION, ViolationKind.fromReport(null));
        assertEquals(ViolationKind.SEPARATION, ViolationKind.fromReport("separation"));
    }

    @Test
    public void ensureAltitudeKindsAreParsedCaseInsensitively() {
        assertEquals(ViolationKind.ALTITUDE_CEILING, ViolationKind.fromReport("ALTITUDE_CEILING"));
        assertEquals(ViolationKind.ALTITUDE_FLOOR, ViolationKind.fromReport("  altitude_floor "));
    }

    @Test
    public void ensureUnrecognisedKindBecomesUnknown() {
        assertEquals(ViolationKind.UNKNOWN, ViolationKind.fromReport("wake_turbulence"));
    }

    @Test
    public void ensureIsAltitudeOnlyTrueForCeilingAndFloor() {
        assertTrue(ViolationKind.ALTITUDE_CEILING.isAltitude());
        assertTrue(ViolationKind.ALTITUDE_FLOOR.isAltitude());
        assertFalse(ViolationKind.SEPARATION.isAltitude());
        assertFalse(ViolationKind.UNKNOWN.isAltitude());
    }

    @Test
    public void ensureEnumValuesAreReachable() {
        assertEquals(4, ViolationKind.values().length);
        assertEquals(ViolationKind.SEPARATION, ViolationKind.valueOf("SEPARATION"));
    }
}