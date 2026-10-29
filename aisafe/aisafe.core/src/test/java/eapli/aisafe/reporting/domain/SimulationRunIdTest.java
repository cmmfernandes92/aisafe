package eapli.aisafe.reporting.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class SimulationRunIdTest {

    @Test
    public void ensureNewIdMintsAUniqueNonNullId() {
        final SimulationRunId a = SimulationRunId.newId();
        final SimulationRunId b = SimulationRunId.newId();

        assertNotNull(a.id());
        assertNotEquals(a, b);
    }

    @Test
    public void ensureValueOfReconstructsFromUuidString() {
        final UUID uuid = UUID.randomUUID();
        final SimulationRunId rid = SimulationRunId.valueOf(uuid.toString());

        assertEquals(uuid, rid.id());
        assertEquals(uuid.toString(), rid.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureValueOfRejectsNull() {
        SimulationRunId.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureValueOfRejectsBlank() {
        SimulationRunId.valueOf("   ");
    }

    @Test
    public void ensureEqualityHashCodeAndComparisonAreConsistent() {
        final UUID uuid = UUID.randomUUID();
        final SimulationRunId a = SimulationRunId.valueOf(uuid.toString());
        final SimulationRunId b = SimulationRunId.valueOf(uuid.toString());

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(0, a.compareTo(b));
        assertFalse(a.equals("not-an-id"));
        assertFalse(a.equals(null));
    }

    @Test
    public void ensureComparisonOrdersByUnderlyingUuid() {
        final SimulationRunId a = SimulationRunId.valueOf("00000000-0000-0000-0000-000000000001");
        final SimulationRunId b = SimulationRunId.valueOf("00000000-0000-0000-0000-000000000002");

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }
}