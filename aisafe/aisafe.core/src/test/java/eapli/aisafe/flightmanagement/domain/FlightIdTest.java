package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import eapli.aisafe.flightroutemanagement.domain.RouteName;

public class FlightIdTest {

    private RouteName route() {
        return RouteName.valueOf("TP123");
    }

    private LocalDate date() {
        return LocalDate.of(2026, 7, 1);
    }

    @Test
    public void ensureFlightIdAcceptsRouteAndDate() {
        final FlightId id = new FlightId(route(), date());

        assertEquals(route(), id.routeName());
        assertEquals(date(), id.departureDate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightIdRejectsNullRoute() {
        new FlightId(null, date());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightIdRejectsNullDate() {
        new FlightId(route(), null);
    }

    @Test
    public void ensureFlightIdEqualsForSameRouteAndDate() {
        assertTrue(new FlightId(route(), date()).equals(new FlightId(route(), date())));
    }

    @Test
    public void ensureFlightIdNotEqualsForDifferentDate() {
        assertFalse(new FlightId(route(), date())
                .equals(new FlightId(route(), LocalDate.of(2026, 7, 2))));
    }

    @Test
    public void ensureFlightIdNotEqualsForDifferentRoute() {
        assertFalse(new FlightId(route(), date())
                .equals(new FlightId(RouteName.valueOf("TP999"), date())));
    }

    @Test
    public void ensureFlightIdEqualsItself() {
        final FlightId id = new FlightId(route(), date());
        assertTrue(id.equals(id));
    }

    @Test
    public void ensureFlightIdNotEqualsOtherType() {
        assertFalse(new FlightId(route(), date()).equals("TP123@2026-07-01"));
    }

    @Test
    public void ensureEqualFlightIdsShareHashCode() {
        assertEquals(new FlightId(route(), date()).hashCode(),
                new FlightId(route(), date()).hashCode());
    }

    @Test
    public void ensureCompareToIsZeroForEqualIds() {
        assertEquals(0, new FlightId(route(), date()).compareTo(new FlightId(route(), date())));
    }

    @Test
    public void ensureCompareToOrdersByRouteFirst() {
        assertTrue(new FlightId(RouteName.valueOf("TP100"), date())
                .compareTo(new FlightId(RouteName.valueOf("TP200"), date())) < 0);
    }

    @Test
    public void ensureCompareToOrdersByDateWhenRouteEqual() {
        assertTrue(new FlightId(route(), date())
                .compareTo(new FlightId(route(), LocalDate.of(2026, 7, 2))) < 0);
    }

    @Test
    public void ensureToStringJoinsRouteAndDate() {
        assertEquals("TP123@2026-07-01", new FlightId(route(), date()).toString());
    }

    @Test
    public void ensureProtectedConstructorExistsForORM() {
        assertTrue(new FlightId() instanceof FlightId);
    }
}
