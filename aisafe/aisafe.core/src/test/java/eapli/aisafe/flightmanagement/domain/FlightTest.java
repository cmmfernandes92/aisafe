package eapli.aisafe.flightmanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;

public class FlightTest {

    private RouteName route() {
        return RouteName.valueOf("TP123");
    }

    private Email pilot() {
        return Email.valueOf("pilot@tap.pt");
    }

    private Registration aircraft() {
        return Registration.valueOf("CS-TUA");
    }

    private LocalDateTime departure() {
        return LocalDateTime.of(2026, 7, 1, 9, 30);
    }

    private Flight aFlight() {
        return new Flight(route(), pilot(), aircraft(), departure());
    }

    // --- constructor: valid input ---

    @Test
    public void ensureFlightAcceptsValidInput() {
        final Flight flight = aFlight();

        assertEquals(route(), flight.route());
        assertEquals(pilot(), flight.pilot());
        assertEquals(aircraft(), flight.aircraft());
        assertEquals(departure(), flight.departure());
    }

    // --- initial plan: DRAFT, empty body (I1) ---

    @Test
    public void ensureNewFlightHasSingleDraftPlan() {
        final Flight flight = aFlight();

        assertEquals(1, flight.numberOfPlans());
        assertEquals(FlightPlanStatus.DRAFT, flight.currentPlan().status());
        assertFalse(flight.hasValidatedPlan());
    }

    @Test
    public void ensureNewDraftPlanHasNoContent() {
        final Flight flight = aFlight();

        assertNull(flight.currentPlan().rawDslContent());
    }

    // --- constructor: null guards ---

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRejectsNullRoute() {
        new Flight(null, pilot(), aircraft(), departure());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRejectsNullPilot() {
        new Flight(route(), null, aircraft(), departure());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRejectsNullAircraft() {
        new Flight(route(), pilot(), null, departure());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFlightRejectsNullDeparture() {
        new Flight(route(), pilot(), aircraft(), null);
    }

    // --- identity: route + date (I4 is structural) ---

    @Test
    public void ensureIdentityIsRouteAndDepartureDate() {
        final Flight flight = aFlight();

        assertEquals(new FlightId(route(), LocalDate.of(2026, 7, 1)), flight.identity());
    }

    @Test
    public void ensureFlightsForSameRouteAndDateAreEqual() {
        final Flight morning = new Flight(route(), pilot(), aircraft(),
                LocalDateTime.of(2026, 7, 1, 9, 30));
        final Flight evening = new Flight(route(), pilot(), aircraft(),
                LocalDateTime.of(2026, 7, 1, 18, 0));

        assertTrue(morning.equals(evening));
        assertTrue(morning.sameAs(evening));
    }

    @Test
    public void ensureFlightsForSameRouteDifferentDateAreNotEqual() {
        final Flight first = new Flight(route(), pilot(), aircraft(),
                LocalDateTime.of(2026, 7, 1, 9, 30));
        final Flight second = new Flight(route(), pilot(), aircraft(),
                LocalDateTime.of(2026, 7, 2, 9, 30));

        assertFalse(first.equals(second));
    }

    // --- invariant: validate current draft plan ---

    @Test
    public void ensureValidatingCurrentPlanMarksItValidated() {
        final Flight flight = aFlight();

        flight.validateCurrentPlan();

        assertTrue(flight.hasValidatedPlan());
        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
    }

    @Test
    public void ensureAddDraftPlanAddsAnotherDraft() {
        final Flight flight = aFlight();

        flight.addDraftPlan();

        assertEquals(2, flight.numberOfPlans());
        assertEquals(FlightPlanStatus.DRAFT, flight.currentPlan().status());
    }

    // --- invariant: at most one VALIDATED plan ---

    @Test(expected = IllegalStateException.class)
    public void ensureFlightRejectsASecondValidatedPlan() {
        final Flight flight = aFlight();
        flight.validateCurrentPlan();

        flight.addDraftPlan();
        flight.validateCurrentPlan();
    }

    // --- US121: import DSL into the current draft and promote it ---

    @Test
    public void ensureImportDslSupersedesDraftAndValidatesIt() {
        final Flight flight = aFlight();

        flight.importDsl("flight TP123 { ... }", "pilot@tap.pt");

        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
        assertEquals("flight TP123 { ... }", flight.currentPlan().rawDslContent());
        assertEquals("pilot@tap.pt", flight.currentPlan().importedBy());
        assertTrue(flight.hasValidatedPlan());
    }

    @Test
    public void ensureReimportInstallsNewValidatedPlanAndDemotesThePreviousOneToDraft() {
        final Flight flight = aFlight();
        flight.importDsl("flight V1 { ... }", "pilot@tap.pt");

        flight.importDsl("flight V2 { ... }", "pilot2@tap.pt");

        assertEquals(2, flight.numberOfPlans());
        // the newest import is the single VALIDATED plan
        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
        assertEquals("flight V2 { ... }", flight.currentPlan().rawDslContent());
        // the previously-validated plan is demoted to DRAFT, keeping its body
        assertEquals(FlightPlanStatus.DRAFT, flight.plans().get(0).status());
        assertEquals("flight V1 { ... }", flight.plans().get(0).rawDslContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportDslRejectsEmptyContent() {
        aFlight().importDsl("", "pilot@tap.pt");
    }

    // --- US085: test current plan ---

    @Test
    public void ensureTestCurrentPlanWithSuccessPromotesPlanToTested() {
        final Flight flight = aFlight();
        flight.importDsl("flight TP123 { ... }", "pilot@tap.pt");
        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());

        final FlightPlanTestResult result = flight.testCurrentPlan(
                (f, p) -> FlightPlanTestResult.success("ok"));

        assertTrue(result.isSuccess());
        assertEquals(FlightPlanStatus.TESTED, flight.currentPlan().status());
    }

    @Test
    public void ensureTestCurrentPlanWithFailureKeepsPlanValidated() {
        final Flight flight = aFlight();
        flight.importDsl("flight TP123 { ... }", "pilot@tap.pt");
        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());

        final FlightPlanTestResult result = flight.testCurrentPlan(
                (f, p) -> FlightPlanTestResult.failure("simulation error"));

        assertFalse(result.isSuccess());
        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureTestCurrentPlanRejectsDraftPlan() {
        aFlight().testCurrentPlan((f, p) -> FlightPlanTestResult.success("ok"));
    }

    // --- equality / ORM / toString ---

    @Test
    public void ensureHashCodeIsConsistent() {
        final Flight flight = aFlight();
        assertEquals(flight.hashCode(), flight.hashCode());
    }

    @Test
    public void ensureProtectedConstructorExistsForORM() {
        assertTrue(new Flight() instanceof Flight);
    }

    @Test
    public void ensureToStringContainsRouteName() {
        assertTrue(aFlight().toString().contains("TP123"));
    }
}
