package eapli.aisafe.enginemodelmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EngineModelNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelNameRejectsNull() {
        new EngineModelName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelNameRejectsEmpty() {
        new EngineModelName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEngineModelNameRejectsBlank() {
        new EngineModelName("   ");
    }

    @Test
    public void ensureEngineModelNameAcceptsValidValue() {
        final EngineModelName name = new EngineModelName("CFM56-5B");
        assertEquals("CFM56-5B", name.value());
    }

    @Test
    public void ensureEngineModelNameStripsWhitespace() {
        final EngineModelName name = new EngineModelName("  CFM56-5B  ");
        assertEquals("CFM56-5B", name.value());
    }

    @Test
    public void ensureEngineModelNameEqualsForSameValue() {
        assertTrue(new EngineModelName("CFM56-5B").equals(new EngineModelName("CFM56-5B")));
    }

    @Test
    public void ensureEngineModelNameNotEqualsForDifferentValue() {
        assertFalse(new EngineModelName("CFM56-5B").equals(new EngineModelName("LEAP-1A")));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final EngineModelName n = new EngineModelName("CFM56-5B");
        assertTrue(n.equals(n));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(new EngineModelName("CFM56-5B").equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(new EngineModelName("CFM56-5B").equals("CFM56-5B"));
    }

    @Test
    public void ensureHashCodeIsConsistent() {
        final EngineModelName n = new EngineModelName("CFM56-5B");
        assertEquals(n.hashCode(), n.hashCode());
    }

    @Test
    public void ensureHashCodeEqualForEqualNames() {
        assertEquals(new EngineModelName("CFM56-5B").hashCode(), new EngineModelName("CFM56-5B").hashCode());
    }

    @Test
    public void ensureCompareToReturnsNegativeForLesser() {
        assertTrue(new EngineModelName("CFM56-5B").compareTo(new EngineModelName("LEAP-1A")) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveForGreater() {
        assertTrue(new EngineModelName("LEAP-1A").compareTo(new EngineModelName("CFM56-5B")) > 0);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, new EngineModelName("CFM56-5B").compareTo(new EngineModelName("CFM56-5B")));
    }
}
