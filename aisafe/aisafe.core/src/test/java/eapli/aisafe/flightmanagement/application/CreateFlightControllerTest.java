package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
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
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
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

/**
 * Integration-style tests for the US080 {@link CreateFlightController}, wired with
 * fake repositories pre-populated via {@code given...} helpers. Each test maps to
 * one acceptance criterion / invariant from the US080 readme, including I6 (route
 * deactivated as of the departure date) over US074's date-based deactivation.
 */
public class CreateFlightControllerTest {

    private static final Email PILOT_EMAIL = Email.valueOf(AuthzTestHelper.PILOT + "@aisafe.test");
    private static final AircraftModel A320 = model("A320neo", "Airbus");

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    private final FakeFlightRepository flightRepository = new FakeFlightRepository();
    private final FakeFlightRouteRepository flightRouteRepository = new FakeFlightRouteRepository();
    private final FakeAircraftRepository aircraftRepository = new FakeAircraftRepository();
    private final FakeAircraftModelRepository aircraftModelRepository = new FakeAircraftModelRepository();
    private final FakePilotRepository pilotRepository = new FakePilotRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();

    private final CreateFlightController controller = new CreateFlightController(
            flightRepository, flightRouteRepository, aircraftRepository, aircraftModelRepository,
            pilotRepository, userRepository);

    @Before
    public void setUpSession() {
        AuthzTestHelper.loginAsPilot();
        final SystemUser pilotSystemUser = AuthzRegistry.authorizationService()
                .session().orElseThrow().authenticatedUser();
        userRepository.save(new User(pilotSystemUser, PILOT_EMAIL, PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
    }

    @After
    public void clearSession() {
        AuthzTestHelper.clearSession();
    }

    @Test
    public void ensureCreateDraftFlightHappyPath() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        final Flight flight = controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);

        assertEquals(FlightPlanStatus.DRAFT, flight.currentPlan().status());
        assertEquals(routeName, flight.route());
        assertEquals(reg, flight.aircraft());
        assertTrue(flightRepository.ofIdentity(flight.identity()).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsPilotFromAnotherCompany() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "FR");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsAircraftFromAnotherCompany() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("F-GZCP");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "FR");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsRetiredAircraft() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenRetiredAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsSecondFlightSameRouteSameDay() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
        controller.createDraftFlight(routeName, departure("2026-07-01T18:00"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsPilotNotCertifiedForAircraftModel() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyNotCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsRouteDeactivatedOnDepartureDate() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenRouteDeactivatedFrom(routeName, "TP", "2026-06-01");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureCreateDraftFlightRequiresPilotRole() {
        AuthzTestHelper.loginAsAdmin();
        controller.createDraftFlight(RouteName.valueOf("TP123"), departure("2026-07-01T09:30"),
                Registration.valueOf("CS-TUA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsUnknownRoute() {
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(RouteName.valueOf("TP999"), departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsUnknownAircraft() {
        final RouteName routeName = RouteName.valueOf("TP123");
        givenActiveRouteForCompany(routeName, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), Registration.valueOf("CS-XXX"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateDraftFlightRejectsMissingPilotProfile() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        // no pilot profile saved

        controller.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureCreateDraftFlightFailsWhenUserProfileMissing() {
        final RouteName routeName = RouteName.valueOf("TP123");
        final Registration reg = Registration.valueOf("CS-TUA");
        givenActiveRouteForCompany(routeName, "TP");
        givenActiveAircraftForCompany(reg, "TP");
        givenPilotForCompanyCertifiedFor(PILOT_EMAIL, "TP");

        final CreateFlightController local = new CreateFlightController(flightRepository,
                flightRouteRepository, aircraftRepository, aircraftModelRepository, pilotRepository,
                new FakeUserRepository());
        local.createDraftFlight(routeName, departure("2026-07-01T09:30"), reg);
    }

    // ---- given... helpers ----

    private void givenActiveRouteForCompany(final RouteName routeName, final String iata) {
        final AirTransportCompany company = new AirTransportCompany(CompanyIATACode.valueOf(iata),
                CompanyICAOCode.valueOf("TAP"), "Carrier " + iata);
        flightRouteRepository.save(new FlightRoute(routeName, company, lisbon(), porto()));
    }

    private void givenRouteDeactivatedFrom(final RouteName routeName, final String iata, final String from) {
        final AirTransportCompany company = new AirTransportCompany(CompanyIATACode.valueOf(iata),
                CompanyICAOCode.valueOf("TAP"), "Carrier " + iata);
        final FlightRoute route = new FlightRoute(routeName, company, lisbon(), porto());
        route.deactivate(LocalDate.parse(from));
        flightRouteRepository.save(route);
    }

    private void givenActiveAircraftForCompany(final Registration reg, final String iata) {
        final long modelId = aircraftModelRepository.saveAndGetId(A320);
        aircraftRepository.save(new Aircraft(reg, modelId, CompanyIATACode.valueOf(iata),
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)), new Country("Portugal"), 0));
    }

    private void givenRetiredAircraftForCompany(final Registration reg, final String iata) {
        final long modelId = aircraftModelRepository.saveAndGetId(A320);
        final Aircraft aircraft = new Aircraft(reg, modelId, CompanyIATACode.valueOf(iata),
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)), new Country("Portugal"), 0);
        aircraft.decommission();
        aircraftRepository.save(aircraft);
    }

    private void givenPilotForCompanyCertifiedFor(final Email email, final String iata) {
        pilotRepository.save(new Pilot(email, CompanyIATACode.valueOf(iata), Set.of(A320)));
    }

    private void givenPilotForCompanyNotCertifiedFor(final Email email, final String iata) {
        pilotRepository.save(new Pilot(email, CompanyIATACode.valueOf(iata),
                Set.of(model("787-9", "Boeing"))));
    }

    private static LocalDateTime departure(final String isoDateTime) {
        return LocalDateTime.parse(isoDateTime);
    }

    private static AircraftModel model(final String name, final String makerName) {
        final Maker maker = Maker.valueOf(makerName);
        final EngineModel engine = new EngineModel(EngineModelName.valueOf(name + "-engine"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833, 122.6,
                0.024, 1.5, 6300, Set.of(engine));
    }

    private static Airport lisbon() {
        return new Airport(new AirportICAOCode("LPPT"), new AirportIATACode("LIS"),
                new AirportName("Humberto Delgado"), new Town("Lisbon"),
                new Elevation(114.0), new Coordinates(38.7742, -9.1342),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    private static Airport porto() {
        return new Airport(new AirportICAOCode("LPPR"), new AirportIATACode("OPO"),
                new AirportName("Francisco Sa Carneiro"), new Town("Porto"),
                new Elevation(69.0), new Coordinates(41.2356, -8.6781),
                new CountryCode("351"), new AreaCode("LPPC"));
    }

    // ---- fake repositories ----

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
        public Optional<Flight> ofIdentity(final FlightId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.containsKey(new FlightId(route, date));
        }

        @Override
        public Iterable<Flight> flightsOf(final eapli.aisafe.usermanagement.domain.Email pilot) {
            final java.util.List<Flight> result = new java.util.ArrayList<>();
            for (final Flight f : data.values()) {
                if (f.pilot().equals(pilot)) {
                    result.add(f);
                }
            }
            return result;
        }

        @Override
        public boolean containsOfIdentity(final FlightId id) {
            return data.containsKey(id);
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
        public Iterable<FlightRoute> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final FlightRoute entity) {
            data.remove(entity.routeName());
        }

        @Override
        public void deleteOfIdentity(final RouteName id) {
            data.remove(id);
        }
    }

    private static class FakeAircraftRepository implements AircraftRepository {
        private final Map<Registration, Aircraft> data = new HashMap<>();

        @Override
        public <S extends Aircraft> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Aircraft> ofIdentity(final Registration id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final Registration id) {
            return data.containsKey(id);
        }

        @Override
        public Iterable<Aircraft> findAllByCompany(final CompanyIATACode companyCode) {
            return data.values().stream().filter(a -> a.companyCode().equals(companyCode)).toList();
        }

        @Override
        public Iterable<Aircraft> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Aircraft entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final Registration id) {
            data.remove(id);
        }
    }

    private static class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();
        private long nextId = 1L;

        long saveAndGetId(final AircraftModel entity) {
            final long id = nextId++;
            data.put(id, entity);
            return id;
        }

        @Override
        public <S extends AircraftModel> S save(final S entity) {
            data.put(nextId++, entity);
            return entity;
        }

        @Override
        public Optional<AircraftModel> ofIdentity(final Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Optional<AircraftModel> findByNameAndMaker(final AircraftModelName name, final Maker maker) {
            return data.values().stream()
                    .filter(m -> m.modelName().equals(name) && m.maker().equals(maker))
                    .findFirst();
        }

        @Override
        public boolean existsByNameAndMaker(final AircraftModelName name, final Maker maker) {
            return findByNameAndMaker(name, maker).isPresent();
        }

        @Override
        public Iterable<AircraftModel> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AircraftModel entity) {
            data.values().removeIf(entity::sameAs);
        }

        @Override
        public void deleteOfIdentity(final Long id) {
            data.remove(id);
        }
    }

    private static class FakePilotRepository implements PilotRepository {
        private final Map<Email, Pilot> data = new HashMap<>();

        @Override
        public <S extends Pilot> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Pilot> ofIdentity(final Email id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final Email id) {
            return data.containsKey(id);
        }

        @Override
        public Iterable<Pilot> findAllByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream().filter(p -> p.companyCode().equals(companyCode)).toList();
        }

        @Override
        public Iterable<Pilot> findAllActivePilotsByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream()
                    .filter(p -> p.companyCode().equals(companyCode) && p.isActive()).toList();
        }

        @Override
        public Iterable<Pilot> findAllActivePilots() {
            return data.values().stream().filter(Pilot::isActive).toList();
        }

        @Override
        public Iterable<Pilot> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Pilot entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final Email id) {
            data.remove(id);
        }
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
        public Optional<User> ofIdentity(final Email id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Iterable<User> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final User entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final Email id) {
            data.remove(id);
        }
    }
}