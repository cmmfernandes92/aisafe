package eapli.aisafe.flightroutemanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RouteNameTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsNull() {
        new RouteName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsEmpty() {
        new RouteName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsOneLetter() {
        RouteName.valueOf("T123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsThreeLetters() {
        RouteName.valueOf("TAP1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsNoDigits() {
        RouteName.valueOf("TP");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsFiveDigits() {
        RouteName.valueOf("TP12345");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRouteNameRejectsDigitsBeforeLetters() {
        RouteName.valueOf("12TP");
    }

    @Test
    public void ensureRouteNameAcceptsSingleDigit() {
        final RouteName name = RouteName.valueOf("TP1");
        assertEquals("TP1", name.value());
    }

    @Test
    public void ensureRouteNameAcceptsFourDigits() {
        final RouteName name = RouteName.valueOf("TP1234");
        assertEquals("TP1234", name.value());
    }

    @Test
    public void ensureRouteNameNormalisesToUpperCase() {
        final RouteName name = RouteName.valueOf("tp123");
        assertEquals("TP123", name.value());
    }

    @Test
    public void ensureRouteNameEqualsForSameValue() {
        assertTrue(RouteName.valueOf("TP123").equals(RouteName.valueOf("TP123")));
    }

    @Test
    public void ensureRouteNameEqualsIsCaseInsensitive() {
        assertTrue(RouteName.valueOf("tp123").equals(RouteName.valueOf("TP123")));
    }

    @Test
    public void ensureRouteNameNotEqualsForDifferentValue() {
        assertFalse(RouteName.valueOf("TP123").equals(RouteName.valueOf("TP456")));
    }

    @Test
    public void ensureEqualsReturnsTrueForSameInstance() {
        final RouteName name = RouteName.valueOf("TP123");
        assertTrue(name.equals(name));
    }

    @Test
    public void ensureEqualsReturnsFalseForNull() {
        assertFalse(RouteName.valueOf("TP123").equals(null));
    }

    @Test
    public void ensureEqualsReturnsFalseForDifferentType() {
        assertFalse(RouteName.valueOf("TP123").equals("TP123"));
    }

    @Test
    public void ensureHashCodeConsistentForSameValue() {
        assertEquals(RouteName.valueOf("TP123").hashCode(), RouteName.valueOf("TP123").hashCode());
    }

    @Test
    public void ensureCompareToReturnsNegativeForLesser() {
        assertTrue(RouteName.valueOf("TP1").compareTo(RouteName.valueOf("TP2")) < 0);
    }

    @Test
    public void ensureCompareToReturnsPositiveForGreater() {
        assertTrue(RouteName.valueOf("TP2").compareTo(RouteName.valueOf("TP1")) > 0);
    }

    @Test
    public void ensureCompareToReturnsZeroForEqual() {
        assertEquals(0, RouteName.valueOf("TP123").compareTo(RouteName.valueOf("TP123")));
    }

    @Test
    public void ensureToStringReturnsValue() {
        assertEquals("TP123", RouteName.valueOf("TP123").toString());
    }
}