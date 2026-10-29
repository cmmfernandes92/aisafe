package eapli.aisafe.flightroutemanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public class DeactivateFlightRouteControllerTest {

    private static final CompanyIATACode TP_IATA = CompanyIATACode.valueOf("TP");
    private static final CompanyIATACode FR_IATA = CompanyIATACode.valueOf("FR");
    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final LocalDate FUTURE = LocalDate.of(2026, 7, 1);

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    private final FakeFlightRouteRepository routeRepository = new FakeFlightRouteRepository();
    private final FakeFlightRepository flightRepository = new FakeFlightRepository();
    private final FakeCompanyRepository companyRepository = new FakeCompanyRepository();
    private final FakeCollaboratorRepository collaboratorRepository = new FakeCollaboratorRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final DeactivateFlightRouteController controller = new DeactivateFlightRouteController(
            routeRepository, flightRepository, companyRepository,
            collaboratorRepository, userRepository);

    private AirTransportCompany tap;

    @Before
    public void setUpSession() {
        AuthzTestHelper.loginAsCompanyCollaborator();
        final SystemUser atccSystemUser = AuthzRegistry.authorizationService()
                .session().orElseThrow().authenticatedUser();
        userRepository.save(new User(atccSystemUser, ATCC_EMAIL,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        tap = new AirTransportCompany(TP_IATA, CompanyICAOCode.valueOf("TAP"), "TAP Air Portugal");
        companyRepository.save(tap);
        collaboratorRepository.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Ops"),
                TP_IATA, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));
    }

    @After
    public void logout() {
        AuthzTestHelper.clearSession();
    }

    private FlightRoute tpRoute(final String name) {
        return new FlightRoute(RouteName.valueOf(name), tap, lisbon(), porto());
    }

    private void saveFlightOnRoute(final String routeName, final LocalDate departureDate) {
        flightRepository.save(new Flight(
                RouteName.valueOf(routeName),
                Email.valueOf("pilot@tap.pt"),
                Registration.valueOf("CS-TFM"),
                departureDate.atTime(10, 0)));
    }

    @Test
    public void ensureDeactivateMarksRouteInactiveAndRecordsDate() {
        routeRepository.save(tpRoute("TP123"));
        final FlightRoute route = controller.deactivateFlightRoute("TP123", FUTURE);
        assertFalse(route.isActive());
        assertEquals(FUTURE, route.deactivatedFrom());
    }

    @Test
    public void ensureDeactivateSucceedsWhenFlightExistsStrictlyBeforeDate() {
        routeRepository.save(tpRoute("TP123"));
        saveFlightOnRoute("TP123", FUTURE.minusDays(1));
        final FlightRoute route = controller.deactivateFlightRoute("TP123", FUTURE);
        assertFalse(route.isActive());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateRejectedWhenFlightExistsOnEffectiveDate() {
        routeRepository.save(tpRoute("TP123"));
        saveFlightOnRoute("TP123", FUTURE);
        controller.deactivateFlightRoute("TP123", FUTURE);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateRejectedWhenFlightExistsAfterEffectiveDate() {
        routeRepository.save(tpRoute("TP123"));
        saveFlightOnRoute("TP123", FUTURE.plusDays(30));
        controller.deactivateFlightRoute("TP123", FUTURE);
    }

    @Test
    public void ensureRouteStaysActiveWhenDeactivationBlockedByFlight() {
        routeRepository.save(tpRoute("TP123"));
        saveFlightOnRoute("TP123", FUTURE);
        try {
            controller.deactivateFlightRoute("TP123", FUTURE);
        } catch (final IllegalStateException expected) { }
        assertTrue(routeRepository.ofIdentity(RouteName.valueOf("TP123")).orElseThrow().isActive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDeactivateRejectsUnknownRoute() {
        controller.deactivateFlightRoute("TP999", FUTURE);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateRejectsAlreadyInactiveRoute() {
        final FlightRoute route = tpRoute("TP123");
        route.deactivate(LocalDate.of(2026, 6, 1));
        routeRepository.save(route);
        controller.deactivateFlightRoute("TP123", FUTURE);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateRejectsRouteOfAnotherCompany() {
        final AirTransportCompany ryanair =
                new AirTransportCompany(FR_IATA, CompanyICAOCode.valueOf("RYR"), "Ryanair");
        routeRepository.save(new FlightRoute(RouteName.valueOf("FR100"), ryanair, lisbon(), porto()));
        controller.deactivateFlightRoute("FR100", FUTURE);
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureDeactivateRequiresAtccRole() {
        AuthzTestHelper.loginAsAdmin();
        controller.deactivateFlightRoute("TP123", FUTURE);
    }

    @Test
    public void ensureActiveRoutesExcludesInactiveOnes() {
        routeRepository.save(tpRoute("TP123"));
        final FlightRoute inactive = tpRoute("TP456");
        inactive.deactivate(LocalDate.of(2026, 6, 1));
        routeRepository.save(inactive);
        int count = 0;
        for (final FlightRoute r : controller.activeRoutesOfMyCompany()) {
            assertTrue(r.isActive());
            count++;
        }
        assertEquals(1, count);
    }

    private Airport lisbon() {
        return new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    private Airport porto() {
        return new Airport(new AirportICAOCode("LPPR"), new AirportIATACode("OPO"),
                new AirportName("Francisco Sa Carneiro"), new Town("Porto"),
                new Elevation(69.0), new Coordinates(41.2356, -8.6781),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    private static class FakeFlightRepository implements FlightRepository {
        private final List<Flight> data = new ArrayList<>();

        @Override public <S extends Flight> S save(final S e) { data.add(e); return e; }
        @Override public boolean existsForRouteOnDate(final RouteName r, final LocalDate d) {
            return data.stream().anyMatch(f -> f.route().equals(r) && f.departure().toLocalDate().equals(d));
        }
        @Override public boolean existsForRouteOnOrAfter(final RouteName r, final LocalDate d) {
            return data.stream().anyMatch(f -> f.route().equals(r) && !f.departure().toLocalDate().isBefore(d));
        }
        @Override public Iterable<Flight> flightsOf(final Email p) {
            return data.stream().filter(f -> f.pilot().equals(p)).toList();
        }
        @Override public Optional<Flight> ofIdentity(final FlightId id) {
            return data.stream().filter(f -> f.identity().equals(id)).findFirst();
        }
        @Override public Iterable<Flight> findAll() { return new ArrayList<>(data); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final Flight e) { data.remove(e); }
        @Override public void deleteOfIdentity(final FlightId id) { data.removeIf(f -> f.identity().equals(id)); }
    }

    private static class FakeFlightRouteRepository implements FlightRouteRepository {
        private final Map<RouteName, FlightRoute> data = new HashMap<>();
        @Override public <S extends FlightRoute> S save(final S e) { data.put(e.routeName(), e); return e; }
        @Override public boolean existsByName(final RouteName n) { return data.containsKey(n); }
        @Override public Iterable<FlightRoute> findByCompany(final AirTransportCompany c) {
            return data.values().stream().filter(r -> r.company().equals(c)).toList();
        }
        @Override public Iterable<FlightRoute> findActiveByCompany(final AirTransportCompany c) {
            return data.values().stream().filter(r -> r.company().equals(c) && r.isActive()).toList();
        }
        @Override public Optional<FlightRoute> ofIdentity(final RouteName id) { return Optional.ofNullable(data.get(id)); }
        @Override public Iterable<FlightRoute> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final FlightRoute e) { data.remove(e.routeName()); }
        @Override public void deleteOfIdentity(final RouteName id) { data.remove(id); }
    }

    private static class FakeCompanyRepository implements AirTransportCompanyRepository {
        private final Map<CompanyIATACode, AirTransportCompany> data = new HashMap<>();
        @Override public <S extends AirTransportCompany> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<AirTransportCompany> findByIataCode(final CompanyIATACode c) { return Optional.ofNullable(data.get(c)); }
        @Override public Optional<AirTransportCompany> findByIcaoCode(final CompanyICAOCode c) {
            return data.values().stream().filter(x -> x.icaoCode().equals(c)).findFirst();
        }
        @Override public Optional<AirTransportCompany> ofIdentity(final CompanyIATACode id) { return findByIataCode(id); }
        @Override public Iterable<AirTransportCompany> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final AirTransportCompany e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final CompanyIATACode id) { data.remove(id); }
    }

    private static class FakeCollaboratorRepository implements CollaboratorRepository {
        private final Map<Email, Collaborator> data = new HashMap<>();
        @Override public <S extends Collaborator> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<Collaborator> ofIdentity(final Email id) { return Optional.ofNullable(data.get(id)); }
        @Override public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email e) {
            return ofIdentity(e).filter(c -> c instanceof CompanyCollaborator).map(c -> (CompanyCollaborator) c);
        }
        @Override public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email e) {
            return ofIdentity(e).filter(c -> c instanceof AreaCollaborator).map(c -> (AreaCollaborator) c);
        }
        @Override public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode code) {
            return data.values().stream().filter(c -> c instanceof CompanyCollaborator).map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(code) && c.isActive()).toList();
        }
        @Override public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode code) {
            return data.values().stream().filter(c -> c instanceof CompanyCollaborator).map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(code)).toList();
        }
        @Override public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode a) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator).map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(a) && c.isActive()).toList();
        }
        @Override public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode a) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator).map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(a)).toList();
        }
        @Override public Iterable<Collaborator> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final Collaborator e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final Email id) { data.remove(id); }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<Email, User> data = new HashMap<>();
        @Override public <S extends User> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<User> findBySystemUser(final SystemUser s) {
            return data.values().stream().filter(u -> u.systemUser().equals(s)).findFirst();
        }
        @Override public Optional<User> ofIdentity(final Email id) { return Optional.ofNullable(data.get(id)); }
        @Override public Iterable<User> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final User e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final Email id) { data.remove(id); }
    }
}
