package eapli.aisafe.fleetmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class RegistrationTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegistrationRejectsNullValue() {
        new Registration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegistrationRejectsEmptyValue() {
        new Registration("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegistrationRejectsBlankValue() {
        new Registration("   ");
    }

    @Test
    public void ensureRegistrationAcceptsValidValue() {
        final Registration reg = new Registration("CS-TNA");
        assertNotNull(reg);
        assertEquals("CS-TNA", reg.value());
    }

    @Test
    public void ensureRegistrationNormalisesToUpperCase() {
        final Registration reg = Registration.valueOf("cs-tna");
        assertEquals("CS-TNA", reg.value());
    }

    @Test
    public void ensureValueOfIsEquivalentToConstructor() {
        assertEquals(new Registration("CS-TNA"), Registration.valueOf("CS-TNA"));
    }

    @Test
    public void ensureToStringReturnsValue() {
        assertEquals("CS-TNA", new Registration("CS-TNA").toString());
    }
}
