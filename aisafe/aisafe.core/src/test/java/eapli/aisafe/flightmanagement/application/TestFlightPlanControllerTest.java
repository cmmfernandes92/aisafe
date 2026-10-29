package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class TestFlightPlanControllerTest {

    // Same fixture used in ImportFlightPlanControllerTest
    private static final String DSL = "flight TP123 {\n  type: regular\n  ...\n}";
    private static final RouteName TP123 = RouteName.valueOf("TP123");
    private static final LocalDate MAY_20 = LocalDate.of(2026, 5, 20);

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void login() {
        AuthzTestHelper.loginAsPilot();
    }

    @After
    public void logout() {
        AuthzTestHelper.clearSession();
    }

    private final FakeFlightRepository flightRepository = new FakeFlightRepository();
    private final TestFlightPlanController controller = new TestFlightPlanController(
            flightRepository, (flight, plan) -> FlightPlanTestResult.success("ok"));

    private Flight givenValidatedFlight(final RouteName route, final LocalDate date) {
        final Flight flight = new Flight(route, Email.valueOf("pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), date.atTime(8, 30));
        flight.importDsl(DSL, "pilot@aisafe.test");
        return flightRepository.save(flight);
    }

    @Test
    public void ensureSuccessfulTestPromotesPlanToTested() {
        final Flight saved = givenValidatedFlight(TP123, MAY_20);
        assertEquals(FlightPlanStatus.VALIDATED, saved.currentPlan().status());

        final FlightPlanTestResult result = controller.testFlightPlan(new FlightId(TP123, MAY_20));

        assertTrue(result.isSuccess());
        assertEquals(FlightPlanStatus.TESTED,
                flightRepository.ofIdentity(new FlightId(TP123, MAY_20)).get().currentPlan().status());
    }

    @Test
    public void ensureFailedTestKeepsPlanValidated() {
        final Flight saved = givenValidatedFlight(TP123, MAY_20);
        assertEquals(FlightPlanStatus.VALIDATED, saved.currentPlan().status());
        final TestFlightPlanController failing = new TestFlightPlanController(
                flightRepository, (flight, plan) -> FlightPlanTestResult.failure("simulation failed"));

        final FlightPlanTestResult result = failing.testFlightPlan(new FlightId(TP123, MAY_20));

        assertFalse(result.isSuccess());
        assertEquals(FlightPlanStatus.VALIDATED,
                flightRepository.ofIdentity(new FlightId(TP123, MAY_20)).get().currentPlan().status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTestIsRejectedWhenNoMatchingFlightExists() {
        controller.testFlightPlan(new FlightId(RouteName.valueOf("TP999"), MAY_20));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureTestIsRejectedWhenPlanIsNotValidated() {
        final Flight draftFlight = new Flight(TP123, Email.valueOf("pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), MAY_20.atTime(8, 30));
        flightRepository.save(draftFlight);

        controller.testFlightPlan(new FlightId(TP123, MAY_20));
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureTestRequiresPilotRole() {
        givenValidatedFlight(TP123, MAY_20);
        AuthzTestHelper.loginAsAdmin();

        controller.testFlightPlan(new FlightId(TP123, MAY_20));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTestIsRejectedForAFlightOwnedByAnotherPilot() {
        final Flight others = new Flight(TP123, Email.valueOf("other.pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), MAY_20.atTime(8, 30));
        others.importDsl(DSL, "other.pilot@aisafe.test");
        flightRepository.save(others);

        controller.testFlightPlan(new FlightId(TP123, MAY_20));
    }

    // ---- Fake repository ----

    private static class FakeFlightRepository implements FlightRepository {

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }
        private final Map<FlightId, Flight> data = new HashMap<>();

        @Override
        public <S extends Flight> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.containsKey(new FlightId(route, date));
        }

        @Override
        public Iterable<Flight> flightsOf(final Email pilot) {
            final java.util.List<Flight> result = new java.util.ArrayList<>();
            for (final Flight f : data.values()) {
                if (f.pilot().equals(pilot)) {
                    result.add(f);
                }
            }
            return result;
        }

        @Override
        public Optional<Flight> ofIdentity(final FlightId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<Flight> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Flight entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightId id) {
            data.remove(id);
        }
    }
}
