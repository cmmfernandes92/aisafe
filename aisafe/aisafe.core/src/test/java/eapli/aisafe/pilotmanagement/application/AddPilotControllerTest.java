package eapli.aisafe.pilotmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
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
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public class AddPilotControllerTest {

    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final CompanyIATACode TP_IATA = CompanyIATACode.valueOf("TP");

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    private final FakePilotRepository pilotRepository = new FakePilotRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakeCompanyRepository companyRepository = new FakeCompanyRepository();
    private final FakeAircraftModelRepository aircraftModelRepository = new FakeAircraftModelRepository();
    private final FakeCollaboratorRepository collaboratorRepository = new FakeCollaboratorRepository();
    private final AddPilotController controller = new AddPilotController(
            pilotRepository, userRepository, companyRepository, aircraftModelRepository, collaboratorRepository);

    @Before
    public void setUpSession() {
        AuthzTestHelper.loginAsCompanyCollaborator();

        final SystemUser atccSystemUser = AuthzRegistry.authorizationService()
                .session().orElseThrow().authenticatedUser();
        userRepository.save(new User(atccSystemUser, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        companyRepository.save(new AirTransportCompany(TP_IATA, CompanyICAOCode.valueOf("TAP"), "TAP Air Portugal"));
        collaboratorRepository.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Ops"), TP_IATA,
                ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));
    }

    @After
    public void clearSession() {
        AuthzTestHelper.clearSession();
    }

    @Test
    public void ensureAddPilotHappyPath() {
        final Email pilotEmail = Email.valueOf("pilot@tap.pt");
        givenExistingUserWithRole(pilotEmail, AisafeRoles.PILOT);
        final long modelId = aircraftModelRepository.saveAndGetId(sampleModel("A320neo"));

        final Pilot pilot = controller.addPilot(pilotEmail.toString(), Set.of(modelId));

        assertEquals(pilotEmail, pilot.identity());
        assertEquals(TP_IATA, pilot.companyCode());
        assertTrue(pilot.isActive());
        assertEquals(1, pilot.certifications().size());
        assertTrue(pilotRepository.containsOfIdentity(pilotEmail));
    }

    @Test
    public void ensureListAircraftModelsReturnsAvailableModels() {
        aircraftModelRepository.saveAndGetId(sampleModel("A320neo"));
        assertTrue(controller.listAircraftModels().iterator().hasNext());
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureAddPilotRequiresAtccRole() {
        AuthzTestHelper.loginAsAdmin();
        controller.addPilot("pilot@tap.pt", Set.of(1L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddPilotRejectsUnknownUser() {
        final long modelId = aircraftModelRepository.saveAndGetId(sampleModel("A320neo"));
        controller.addPilot("unknown@tap.pt", Set.of(modelId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddPilotRejectsUserWithoutPilotRole() {
        final Email email = Email.valueOf("admin@tap.pt");
        givenExistingUserWithRole(email, AisafeRoles.ADMIN);
        final long modelId = aircraftModelRepository.saveAndGetId(sampleModel("A320neo"));

        controller.addPilot(email.toString(), Set.of(modelId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddPilotRejectsDuplicatePilot() {
        final Email pilotEmail = Email.valueOf("pilot2@tap.pt");
        givenExistingUserWithRole(pilotEmail, AisafeRoles.PILOT);
        final long modelId = aircraftModelRepository.saveAndGetId(sampleModel("A320neo"));

        controller.addPilot(pilotEmail.toString(), Set.of(modelId));
        controller.addPilot(pilotEmail.toString(), Set.of(modelId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddPilotRejectsMissingCertifications() {
        final Email pilotEmail = Email.valueOf("pilot3@tap.pt");
        givenExistingUserWithRole(pilotEmail, AisafeRoles.PILOT);

        controller.addPilot(pilotEmail.toString(), Set.of());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAddPilotRejectsUnknownAircraftModel() {
        final Email pilotEmail = Email.valueOf("pilot4@tap.pt");
        givenExistingUserWithRole(pilotEmail, AisafeRoles.PILOT);

        controller.addPilot(pilotEmail.toString(), Set.of(999L));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureAddPilotFailsWhenLoggedUserProfileMissing() {
        final AddPilotController local = new AddPilotController(
                pilotRepository, new FakeUserRepository(), companyRepository, aircraftModelRepository,
                collaboratorRepository);
        local.addPilot("pilot@tap.pt", Set.of(1L));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureAddPilotFailsWhenCompanyCollaboratorProfileMissing() {
        final AddPilotController local = new AddPilotController(
                pilotRepository, userRepository, companyRepository, aircraftModelRepository,
                new FakeCollaboratorRepository());
        local.addPilot("pilot@tap.pt", Set.of(1L));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureAddPilotFailsWhenCompanyMissing() {
        final AddPilotController local = new AddPilotController(
                pilotRepository, userRepository, new FakeCompanyRepository(), aircraftModelRepository,
                collaboratorRepository);
        local.addPilot("pilot@tap.pt", Set.of(1L));
    }

    private void givenExistingUserWithRole(final Email email, final Role role) {
        final SystemUser systemUser = UserBuilderHelper.builder()
                .withUsername(email.toString())
                .withPassword("Password1")
                .withName("Test", "Pilot")
                .withEmail(email.toString())
                .withRoles(role)
                .build();
        userRepository.save(new User(systemUser, email, PhoneNumber.valueOf("+351910000001"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(2)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(2))));
    }

    private AircraftModel sampleModel(final String name) {
        final Maker maker = Maker.valueOf("Airbus");
        final EngineModel engine = new EngineModel(EngineModelName.valueOf(name + "-engine"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833, 122.6,
                0.024, 1.5, 6300, Set.of(engine));
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
        public Optional<AirTransportCompany> ofIdentity(final CompanyIATACode id) {
            return findByIataCode(id);
        }

        @Override
        public Iterable<AirTransportCompany> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AirTransportCompany entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final CompanyIATACode id) {
            data.remove(id);
        }
    }

    private static class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();
        private long nextId = 1L;

        long saveAndGetId(final AircraftModel model) {
            final long id = nextId++;
            data.put(id, model);
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
