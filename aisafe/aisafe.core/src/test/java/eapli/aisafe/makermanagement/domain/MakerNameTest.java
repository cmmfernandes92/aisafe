package eapli.aisafe.makermanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MakerNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureMakerNameRejectsNull() {
        new MakerName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMakerNameRejectsEmpty() {
        new MakerName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMakerNameRejectsBlank() {
        new MakerName("   ");
    }

    @Test
    public void ensureMakerNameAcceptsValidValue() {
        final MakerName name = new MakerName("Airbus");
        assertEquals("Airbus", name.value());
    }

    @Test
    public void ensureMakerNameStripsWhitespace() {
        final MakerName name = new MakerName("  Boeing  ");
        assertEquals("Boeing", name.value());
    }

    @Test
    public void ensureMakerNameEqualsForSameValue() {
        assertTrue(new MakerName("Airbus").equals(new MakerName("Airbus")));
    }

    @Test
    public void ensureMakerNameNotEqualsForDifferentValue() {
        assertFalse(new MakerName("Airbus").equals(new MakerName("Boeing")));
    }
}
