package eapli.aisafe.pilotmanagement.application;

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
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public class DeactivatePilotControllerTest {

    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final CompanyIATACode TP_IATA = CompanyIATACode.valueOf("TP");
    private static final CompanyIATACode FR_IATA = CompanyIATACode.valueOf("FR");
    private static final Email PILOT_EMAIL = Email.valueOf("pilot@tp.pt");

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    private final FakePilotRepository pilotRepository = new FakePilotRepository();
    private final FakeFlightRepository flightRepository = new FakeFlightRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakeCollaboratorRepository collaboratorRepository = new FakeCollaboratorRepository();
    private final DeactivatePilotController controller = new DeactivatePilotController(
            pilotRepository, flightRepository, userRepository, collaboratorRepository);

    @Before
    public void setUpSession() {
        AuthzTestHelper.loginAsCompanyCollaborator();

        final SystemUser atccSystemUser = AuthzRegistry.authorizationService()
                .session().orElseThrow().authenticatedUser();
        userRepository.save(new User(atccSystemUser, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        collaboratorRepository.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Ops"), TP_IATA,
                ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));
    }

    @After
    public void clearSession() {
        AuthzTestHelper.clearSession();
    }

    @Test
    public void ensureDeactivatePilotHappyPath() {
        pilotRepository.save(new Pilot(PILOT_EMAIL, TP_IATA, Set.of(sampleModel("A320neo"))));
        final CapturingReporter reporter = new CapturingReporter();

        controller.deactivatePilot(PILOT_EMAIL.toString(), reporter);

        assertFalse(pilotRepository.ofIdentity(PILOT_EMAIL).orElseThrow().isActive());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivatePilotIsBlockedWhenAssignedFlightsExist() {
        pilotRepository.save(new Pilot(PILOT_EMAIL, TP_IATA, Set.of(sampleModel("A320neo"))));
        flightRepository.save(new Flight(RouteName.valueOf("TP123"), PILOT_EMAIL,
                Registration.valueOf("CS-TFX"), LocalDateTime.now().plusDays(1)));

        controller.deactivatePilot(PILOT_EMAIL.toString(), new CapturingReporter());
    }

    @Test
    public void ensureDeactivatePilotWithFlightsReportsBeforeThrowing() {
        pilotRepository.save(new Pilot(PILOT_EMAIL, TP_IATA, Set.of(sampleModel("A320neo"))));
        flightRepository.save(new Flight(RouteName.valueOf("TP123"), PILOT_EMAIL,
                Registration.valueOf("CS-TFX"), LocalDateTime.now().plusDays(1)));
        final CapturingReporter reporter = new CapturingReporter();

        try {
            controller.deactivatePilot(PILOT_EMAIL.toString(), reporter);
            org.junit.Assert.fail("Expected IllegalStateException");
        } catch (final IllegalStateException e) {
            assertEquals(1, reporter.captured().size());
            assertEquals("TP123", reporter.captured().get(0).routeName());
            assertTrue(pilotRepository.ofIdentity(PILOT_EMAIL).orElseThrow().isActive());
        }
    }

    @Test
    public void ensureDeactivatePilotWithNoFlightsCallsReporterWithEmptyList() {
        pilotRepository.save(new Pilot(PILOT_EMAIL, TP_IATA, Set.of(sampleModel("A320neo"))));
        final CapturingReporter reporter = new CapturingReporter();

        controller.deactivatePilot(PILOT_EMAIL.toString(), reporter);

        assertTrue(reporter.captured().isEmpty());
    }

    @Test
    public void ensureListActivePilotsOnlyReturnsActiveOnesForMyCompany() {
        final Email active = Email.valueOf("active@tp.pt");
        final Email inactive = Email.valueOf("inactive@tp.pt");
        final Pilot activePilot = pilotRepository.save(new Pilot(active, TP_IATA, Set.of(sampleModel("A320neo"))));
        final Pilot inactivePilot = pilotRepository.save(new Pilot(inactive, TP_IATA, Set.of(sampleModel("A321neo"))));
        inactivePilot.deactivate();

        final List<Pilot> result = new ArrayList<>();
        controller.listActivePilotsOfMyCompany().forEach(result::add);

        assertEquals(1, result.size());
        assertEquals(active, result.get(0).identity());
    }

    @Test
    public void ensureListActivePilotsAsPowerUserReturnsAllCompanies() {
        AuthzTestHelper.loginAsPowerUser();
        pilotRepository.save(new Pilot(Email.valueOf("pilot.tp@tp.pt"), TP_IATA, Set.of(sampleModel("A320neo"))));
        pilotRepository.save(new Pilot(Email.valueOf("pilot.fr@fr.pt"), FR_IATA, Set.of(sampleModel("A321neo"))));

        final List<Pilot> result = new ArrayList<>();
        controller.listActivePilotsOfMyCompany().forEach(result::add);

        assertEquals(2, result.size());
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureDeactivatePilotRequiresAtccRole() {
        AuthzTestHelper.loginAsAdmin();
        controller.deactivatePilot(PILOT_EMAIL.toString(), new CapturingReporter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDeactivateUnknownPilotThrows() {
        controller.deactivatePilot("unknown@tp.pt", new CapturingReporter());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDeactivateAlreadyInactivePilotThrows() {
        final Pilot pilot = pilotRepository.save(new Pilot(PILOT_EMAIL, TP_IATA, Set.of(sampleModel("A320neo"))));
        pilot.deactivate();

        controller.deactivatePilot(PILOT_EMAIL.toString(), new CapturingReporter());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureListActivePilotsThrowsWhenUserProfileMissing() {
        userRepository.deleteOfIdentity(ATCC_EMAIL);
        controller.listActivePilotsOfMyCompany();
    }

    @Test(expected = IllegalStateException.class)
    public void ensureListActivePilotsThrowsWhenCompanyCollaboratorMissing() {
        collaboratorRepository.deleteOfIdentity(ATCC_EMAIL);
        controller.listActivePilotsOfMyCompany();
    }

    private AircraftModel sampleModel(final String name) {
        final Maker maker = Maker.valueOf("Airbus");
        final EngineModel engine = new EngineModel(EngineModelName.valueOf(name + "-engine"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833, 122.6,
                0.024, 1.5, 6300, Set.of(engine));
    }

    private static class CapturingReporter implements AssignedFlightsReporter {
        private final List<AssignedFlightDTO> captured = new ArrayList<>();

        @Override
        public void report(final Iterable<AssignedFlightDTO> flights) {
            flights.forEach(captured::add);
        }

        public List<AssignedFlightDTO> captured() {
            return captured;
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
        public boolean containsOfIdentity(final FlightId id) {
            return data.containsKey(id);
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.keySet().stream()
                    .anyMatch(id -> id.routeName().equals(route) && id.departureDate().equals(date));
        }

        @Override
        public Iterable<Flight> flightsOf(final Email pilot) {
            return data.values().stream().filter(f -> f.pilot().equals(pilot)).toList();
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
                    .filter(c -> c.companyCode().equals(companyCode) && c.isActive())
                    .toList();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream().filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode))
                    .toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode) && c.isActive())
                    .toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
            return data.values().stream().filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode))
                    .toList();
        }

        @Override
        public Iterable<Collaborator> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Collaborator entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final Email id) {
            data.remove(id);
        }
    }
}