package eapli.aisafe.flightroutemanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
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
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.countrymanagement.domain.CountryCode;
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

public class CreateFlightRouteControllerTest {

    private static final CompanyIATACode TP_IATA = CompanyIATACode.valueOf("TP");
    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    private final FakeFlightRouteRepository routeRepository = new FakeFlightRouteRepository();
    private final FakeAirportRepository airportRepository = new FakeAirportRepository();
    private final FakeCompanyRepository companyRepository = new FakeCompanyRepository();
    private final FakeCollaboratorRepository collaboratorRepository = new FakeCollaboratorRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final CreateFlightRouteController controller = new CreateFlightRouteController(
            routeRepository, airportRepository, companyRepository, collaboratorRepository, userRepository);

    @Before
    public void setUpSession() {
        AuthzTestHelper.loginAsCompanyCollaborator();
        final SystemUser atccSystemUser = AuthzRegistry.authorizationService()
                .session().orElseThrow().authenticatedUser();
        userRepository.save(new User(atccSystemUser, ATCC_EMAIL,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        companyRepository.save(new AirTransportCompany(TP_IATA, CompanyICAOCode.valueOf("TAP"), "TAP Air Portugal"));
        collaboratorRepository.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Ops"),
                TP_IATA, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));
    }

    @After
    public void logout() {
        AuthzTestHelper.clearSession();
    }

    @Test
    public void ensureCreateFlightRouteHappyPathWithIataAirportCodes() {
        airportRepository.save(lisbon());
        airportRepository.save(porto());

        final FlightRoute route = controller.createFlightRoute("TP123", "LIS", "OPO");

        assertEquals(RouteName.valueOf("TP123"), route.routeName());
        assertTrue(route.isActive());
        assertTrue(routeRepository.existsByName(RouteName.valueOf("TP123")));
    }

    @Test
    public void ensureCreateFlightRouteHappyPathWithIcaoAirportCodes() {
        airportRepository.save(lisbon());
        airportRepository.save(porto());

        final FlightRoute route = controller.createFlightRoute("TP456", "LPPT", "LPPR");

        assertEquals(RouteName.valueOf("TP456"), route.routeName());
        assertTrue(route.isActive());
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureCreateFlightRouteRequiresAtccRole() {
        AuthzTestHelper.loginAsAdmin();
        controller.createFlightRoute("TP123", "LIS", "OPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsInvalidRouteNameFormat() {
        controller.createFlightRoute("INVALID", "LIS", "OPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsWrongIataPrefix() {
        controller.createFlightRoute("FR100", "LIS", "OPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsDuplicateRouteName() {
        airportRepository.save(lisbon());
        airportRepository.save(porto());

        controller.createFlightRoute("TP001", "LIS", "OPO");
        controller.createFlightRoute("TP001", "LPPT", "LPPR");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsUnknownOriginAirport() {
        airportRepository.save(porto());
        controller.createFlightRoute("TP001", "LIS", "OPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsUnknownDestinationAirport() {
        airportRepository.save(lisbon());
        controller.createFlightRoute("TP001", "LIS", "OPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsSameOriginAndDestination() {
        airportRepository.save(lisbon());
        controller.createFlightRoute("TP001", "LIS", "LIS");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateFlightRouteRejectsUnrecognisedAirportCodeLength() {
        controller.createFlightRoute("TP001", "LI", "OPO");
    }

    @Test(expected = IllegalStateException.class)
    public void ensureCreateFlightRouteFailsWhenUserProfileNotFound() {
        final CreateFlightRouteController local = new CreateFlightRouteController(
                new FakeFlightRouteRepository(), new FakeAirportRepository(),
                companyRepository, collaboratorRepository, new FakeUserRepository());
        local.createFlightRoute("TP001", "LIS", "OPO");
    }

    @Test(expected = IllegalStateException.class)
    public void ensureCreateFlightRouteFailsWhenCollaboratorProfileNotFound() {
        final CreateFlightRouteController local = new CreateFlightRouteController(
                new FakeFlightRouteRepository(), new FakeAirportRepository(),
                companyRepository, new FakeCollaboratorRepository(), userRepository);
        local.createFlightRoute("TP001", "LIS", "OPO");
    }

    @Test(expected = IllegalStateException.class)
    public void ensureCreateFlightRouteFailsWhenCompanyNotFound() {
        final CreateFlightRouteController local = new CreateFlightRouteController(
                new FakeFlightRouteRepository(), new FakeAirportRepository(),
                new FakeCompanyRepository(), collaboratorRepository, userRepository);
        local.createFlightRoute("TP001", "LIS", "OPO");
    }

    // ---- Airport fixtures (same as in FlightRouteTest) ----

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

    // ---- Fake repositories ----

    private static class FakeFlightRouteRepository implements FlightRouteRepository {

        @Override
        public Iterable<eapli.aisafe.flightroutemanagement.domain.FlightRoute> findActiveByCompany(eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany company) {
            return java.util.Collections.emptyList();
        }
        private final Map<RouteName, FlightRoute> data = new HashMap<>();

        @Override
        public <S extends FlightRoute> S save(final S entity) {
            data.put(entity.routeName(), entity);
            return entity;
        }

        @Override
        public boolean existsByName(final RouteName routeName) {
            return data.containsKey(routeName);
        }

        @Override
        public Iterable<FlightRoute> findByCompany(final AirTransportCompany company) {
            return data.values().stream().filter(r -> r.company().equals(company)).toList();
        }

        @Override
        public Optional<FlightRoute> ofIdentity(final RouteName id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<FlightRoute> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final FlightRoute entity) { data.remove(entity.routeName()); }

        @Override
        public void deleteOfIdentity(final RouteName id) { data.remove(id); }
    }

    private static class FakeAirportRepository implements AirportRepository {
        private final Map<AirportICAOCode, Airport> data = new HashMap<>();

        @Override
        public <S extends Airport> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
            return Optional.ofNullable(data.get(icaoCode));
        }

        @Override
        public Optional<Airport> findByIataCode(final AirportIATACode iataCode) {
            return data.values().stream().filter(a -> a.iataCode().equals(iataCode)).findFirst();
        }

        @Override
        public Iterable<Airport> findByAreaCode(final AreaCode areaCode) {
            return data.values().stream().filter(a -> a.areaCode().equals(areaCode)).toList();
        }

        @Override
        public Iterable<Airport> findByCountryCode(final CountryCode countryCode) {
            return data.values().stream().filter(a -> a.countryCode().equals(countryCode)).toList();
        }

        @Override
        public Optional<Airport> ofIdentity(final AirportICAOCode id) { return findByIcaoCode(id); }

        @Override
        public Iterable<Airport> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final Airport entity) { data.remove(entity.identity()); }

        @Override
        public void deleteOfIdentity(final AirportICAOCode id) { data.remove(id); }
    }

    private static class FakeCompanyRepository implements AirTransportCompanyRepository {
        private final Map<CompanyIATACode, AirTransportCompany> data = new HashMap<>();

        @Override
        public <S extends AirTransportCompany> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirTransportCompany> findByIataCode(final CompanyIATACode iataCode) {
            return Optional.ofNullable(data.get(iataCode));
        }

        @Override
        public Optional<AirTransportCompany> findByIcaoCode(final CompanyICAOCode icaoCode) {
            return data.values().stream().filter(c -> c.icaoCode().equals(icaoCode)).findFirst();
        }

        @Override
        public Optional<AirTransportCompany> ofIdentity(final CompanyIATACode id) { return findByIataCode(id); }

        @Override
        public Iterable<AirTransportCompany> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final AirTransportCompany entity) { data.remove(entity.identity()); }

        @Override
        public void deleteOfIdentity(final CompanyIATACode id) { data.remove(id); }
    }

    private static class FakeCollaboratorRepository implements CollaboratorRepository {
        private final Map<Email, Collaborator> data = new HashMap<>();

        @Override
        public <S extends Collaborator> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Collaborator> ofIdentity(final Email id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
            return ofIdentity(email).filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c);
        }

        @Override
        public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
            return ofIdentity(email).filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c);
        }

        @Override
        public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream().filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode) && c.isActive()).toList();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream().filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode)).toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode) && c.isActive()).toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode)).toList();
        }

        @Override
        public Iterable<Collaborator> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final Collaborator entity) { data.remove(entity.identity()); }

        @Override
        public void deleteOfIdentity(final Email id) { data.remove(id); }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<Email, User> data = new HashMap<>();

        @Override
        public <S extends User> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<User> findBySystemUser(final SystemUser systemUser) {
            return data.values().stream().filter(u -> u.systemUser().equals(systemUser)).findFirst();
        }

        @Override
        public Optional<User> ofIdentity(final Email id) { return Optional.ofNullable(data.get(id)); }

        @Override
        public Iterable<User> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final User entity) { data.remove(entity.identity()); }

        @Override
        public void deleteOfIdentity(final Email id) { data.remove(id); }
    }
}
