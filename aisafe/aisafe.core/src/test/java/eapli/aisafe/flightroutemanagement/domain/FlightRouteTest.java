package eapli.aisafe.flightroutemanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.domain.AirportName;
import eapli.aisafe.airportmanagement.domain.Elevation;
import eapli.aisafe.airportmanagement.domain.Town;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.countrymanagement.domain.CountryCode;

public class FlightRouteTest {

    private AirTransportCompany tapAirPortugal() {
        return new AirTransportCompany(
                CompanyIATACode.valueOf("TP"),
                CompanyICAOCode.valueOf("TAP"),
                "TAP Air Portugal");
    }

    private Airport lisbon() {
        return new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    private Airport porto() {
        return new Airport(new AirportICAOCode("LPPR"), new AirportIATACode("OPO"),
                new AirportName("Francisco Sá Carneiro"), new Town("Porto"),
                new Elevation(69.0), new Coordinates(41.2356, -8.6781),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    // --- constructor: valid input ---

    @Test
    public void ensureFlightRouteAcceptsValidInput() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"),
                tapAirPortugal(),
                lisbon(),
                porto());

        assertEquals(RouteName.valueOf("TP123"), route.routeName());
        assertEquals(lisbon(), route.origin());
        assertEquals(porto(), route.destination());
        assertTrue(route.isActive());
    }

    // --- constructor: null guards ---

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRouteRejectsNullRouteName() {
        new FlightRoute(null, tapAirPortugal(), lisbon(), porto());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRouteRejectsNullCompany() {
        new FlightRoute(RouteName.valueOf("TP001"), null, lisbon(), porto());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRouteRejectsNullOrigin() {
        new FlightRoute(RouteName.valueOf("TP001"), tapAirPortugal(), null, porto());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRouteRejectsNullDestination() {
        new FlightRoute(RouteName.valueOf("TP001"), tapAirPortugal(), lisbon(), null);
    }

    // --- invariant: origin ≠ destination ---

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRouteRejectsSameOriginDestination() {
        new FlightRoute(RouteName.valueOf("TP001"), tapAirPortugal(), lisbon(), lisbon());
    }

    // --- active flag ---

    @Test
    public void ensureFlightRouteIsActiveOnCreation() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP001"), tapAirPortugal(), lisbon(), porto());
        assertTrue(route.isActive());
    }

    // --- identity ---

    @Test
    public void ensureIdentityReturnsRouteName() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        assertEquals(RouteName.valueOf("TP123"), route.identity());
    }

    // --- equality ---

    @Test
    public void ensureFlightRouteEqualsForSameRouteName() {
        final FlightRoute one = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        final FlightRoute another = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), porto(), lisbon());
        assertTrue(one.equals(another));
    }

    @Test
    public void ensureFlightRouteNotEqualsForDifferentRouteName() {
        final FlightRoute one = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        final FlightRoute another = new FlightRoute(
                RouteName.valueOf("TP456"), tapAirPortugal(), lisbon(), porto());
        assertFalse(one.equals(another));
    }

    @Test
    public void ensureFlightRouteSameAsForSameRouteName() {
        final FlightRoute one = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        final FlightRoute another = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), porto(), lisbon());
        assertTrue(one.sameAs(another));
    }

    @Test
    public void ensureHashCodeConsistentForSameRouteName() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        assertEquals(route.hashCode(), route.hashCode());
    }

    // --- toString ---

    @Test
    public void ensureToStringContainsRouteName() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        assertTrue(route.toString().contains("TP123"));
    }

    // --- ORM ---

    @Test
    public void ensureFlightRouteProtectedConstructorExistsForORM() {
        assertTrue(new FlightRoute() instanceof FlightRoute);
    }

    // --- US074: deactivation ---

    @Test
    public void ensureRouteHasNoDeactivationDateOnCreation() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        assertTrue(route.isActive());
        assertNull(route.deactivatedFrom());
    }

    @Test
    public void ensureDeactivateSetsInactiveAndRecordsTheDate() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        final LocalDate from = LocalDate.of(2026, 7, 1);

        route.deactivate(from);

        assertFalse(route.isActive());
        assertEquals(from, route.deactivatedFrom());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDeactivateRejectsNullDate() {
        new FlightRoute(RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto())
                .deactivate(null);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateRejectsAlreadyInactiveRoute() {
        final FlightRoute route = new FlightRoute(
                RouteName.valueOf("TP123"), tapAirPortugal(), lisbon(), porto());
        route.deactivate(LocalDate.of(2026, 7, 1));
        route.deactivate(LocalDate.of(2026, 8, 1));
    }
}