package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class ImportFlightPlanControllerTest {

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
    private final ImportFlightPlanController controller = new ImportFlightPlanController(flightRepository);

    private Flight givenDraftFlight(final RouteName route, final LocalDate date) {
        final Flight flight = new Flight(route, Email.valueOf("pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), date.atTime(8, 30));
        return flightRepository.save(flight);
    }

    @Test
    public void ensureValidImportSupersedesDraftAndPromotesItToValidated() {
        givenDraftFlight(TP123, MAY_20);

        final Flight flight = controller.importFlightPlan(new FlightId(TP123, MAY_20), DSL);

        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
        assertEquals(DSL, flight.currentPlan().rawDslContent());
        assertEquals(AuthzTestHelper.PILOT, flight.currentPlan().importedBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportIsRejectedWhenNoMatchingFlightExists() {
        controller.importFlightPlan(new FlightId(RouteName.valueOf("TP999"), MAY_20), DSL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportIsRejectedForEmptyContent() {
        givenDraftFlight(TP123, MAY_20);
        controller.importFlightPlan(new FlightId(TP123, MAY_20), "");
    }

    @Test
    public void ensureReimportSupersedesAndDemotesThePreviousValidatedPlan() {
        givenDraftFlight(TP123, MAY_20);
        controller.importFlightPlan(new FlightId(TP123, MAY_20), DSL);

        final Flight flight = controller.importFlightPlan(new FlightId(TP123, MAY_20),
                "flight TP123 {\n  type: regular\n  v2\n}");

        assertEquals(FlightPlanStatus.VALIDATED, flight.currentPlan().status());
        assertEquals(2, flight.numberOfPlans());
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureImportRequiresPilotRole() {
        givenDraftFlight(TP123, MAY_20);
        AuthzTestHelper.loginAsAdmin();

        controller.importFlightPlan(new FlightId(TP123, MAY_20), DSL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportIsRejectedForAFlightOwnedByAnotherPilot() {
        final Flight others = new Flight(TP123, Email.valueOf("other.pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), MAY_20.atTime(8, 30));
        flightRepository.save(others);

        controller.importFlightPlan(new FlightId(TP123, MAY_20), DSL);
    }

    @Test
    public void ensureFlightsOfAuthenticatedPilotReturnsOnlyOwnFlights() {
        givenDraftFlight(TP123, MAY_20);
        flightRepository.save(new Flight(RouteName.valueOf("TP999"),
                Email.valueOf("other.pilot@aisafe.test"),
                Registration.valueOf("CS-TUA"), MAY_20.atTime(9, 0)));

        final java.util.List<Flight> mine = new java.util.ArrayList<>();
        controller.flightsOfAuthenticatedPilot().forEach(mine::add);

        assertEquals(1, mine.size());
        assertEquals(TP123, mine.get(0).route());
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