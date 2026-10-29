package eapli.aisafe.fleetmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
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
import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController.FleetEntry;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.makermanagement.domain.Maker;
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

public class ListCompanyFleetControllerTest {

    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final CompanyIATACode COMPANY_TP = CompanyIATACode.valueOf("TP");
    private static final CompanyIATACode COMPANY_BA = CompanyIATACode.valueOf("BA");

    private final FakeAircraftRepository aircraftRepo = new FakeAircraftRepository();
    private final FakeAircraftModelRepository modelRepo = new FakeAircraftModelRepository();
    private final FakeCollaboratorRepository collaboratorRepo = new FakeCollaboratorRepository();
    private final FakeUserRepository userRepo = new FakeUserRepository();

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void setUp() {
        AuthzTestHelper.loginAsCompanyCollaborator();
        final SystemUser systemUser = AuthzRegistry.authorizationService()
                .session().get().authenticatedUser();
        userRepo.save(new User(systemUser, ATCC_EMAIL,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        collaboratorRepo.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Operations Manager"),
                COMPANY_TP, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));
    }

    @After
    public void tearDown() {
        AuthzTestHelper.clearSession();
    }

    private ListCompanyFleetController newController() {
        return new ListCompanyFleetController(aircraftRepo, modelRepo, collaboratorRepo, userRepo);
    }

    private static AircraftModel buildModel(final String name, final String makerName) {
        final Maker maker = Maker.valueOf(makerName);
        final EngineModel engine = new EngineModel(EngineModelName.valueOf("CFM56-5B"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf(name), maker,
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                java.util.Set.of(engine));
    }

    private Aircraft seedAircraft(final String reg, final long modelId,
            final CompanyIATACode company, final int economySeats) {
        final Aircraft aircraft = new Aircraft(
                Registration.valueOf(reg),
                modelId,
                company,
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, economySeats)),
                new Country("Portugal"),
                0);
        return aircraftRepo.save(aircraft);
    }

    @Test
    public void ensureUnfilteredListReturnsAllCompanyAircraft() {
        final long modelId = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        seedAircraft("CS-TNA", modelId, COMPANY_TP, 150);
        seedAircraft("CS-TNB", modelId, COMPANY_TP, 130);

        final List<FleetEntry> result = newController().listFleet(null, null, null);
        assertEquals(2, result.size());
    }

    @Test
    public void ensureOtherCompanyAircraftExcluded() {
        final long modelId = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        seedAircraft("CS-TNA", modelId, COMPANY_TP, 150);
        seedAircraft("G-ABCD", modelId, COMPANY_BA, 150);

        final List<FleetEntry> result = newController().listFleet(null, null, null);
        assertEquals(1, result.size());
        assertEquals(COMPANY_TP, result.get(0).aircraft().companyCode());
    }

    @Test
    public void ensureRetiredAircraftIncludedByDefault() {
        final long modelId = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        seedAircraft("CS-TNA", modelId, COMPANY_TP, 150);
        final Aircraft retired = seedAircraft("CS-TNB", modelId, COMPANY_TP, 130);
        retired.decommission();

        final List<FleetEntry> result = newController().listFleet(null, null, null);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.aircraft().status() == OperationalStatus.RETIRED));
    }

    @Test
    public void ensureEmptyFleetReturnsEmptyList() {
        final List<FleetEntry> result = newController().listFleet(null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void ensureModelNameFilterIsCaseInsensitiveContains() {
        final long a320Id = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        final long b787Id = modelRepo.saveAndGetId(buildModel("787-9", "Boeing"));
        seedAircraft("CS-TNA", a320Id, COMPANY_TP, 150);
        seedAircraft("CS-TNB", b787Id, COMPANY_TP, 200);

        final List<FleetEntry> result = newController().listFleet("a320", null, null);
        assertEquals(1, result.size());
        assertEquals("CS-TNA", result.get(0).aircraft().registration().value());
    }

    @Test
    public void ensureMakerFilterIsCaseInsensitiveContains() {
        final long airbusId = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        final long boeingId = modelRepo.saveAndGetId(buildModel("787-9", "Boeing"));
        seedAircraft("CS-TNA", airbusId, COMPANY_TP, 150);
        seedAircraft("CS-TNB", boeingId, COMPANY_TP, 200);

        final List<FleetEntry> result = newController().listFleet(null, "boe", null);
        assertEquals(1, result.size());
        assertEquals("CS-TNB", result.get(0).aircraft().registration().value());
    }

    @Test
    public void ensureMinCapacityFilterExcludesUndercapacityModels() {
        final AircraftModel smallModel = new AircraftModel(
                AircraftModelName.valueOf("A320neo"), Maker.valueOf("Airbus"),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                java.util.Set.of(new EngineModel(EngineModelName.valueOf("CFM56"),
                        Maker.valueOf("Airbus"), MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54))) {
            @Override
            public int maxPassengerCapacity() { return 150; }
        };
        final AircraftModel largeModel = new AircraftModel(
                AircraftModelName.valueOf("787-9"), Maker.valueOf("Boeing"),
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                java.util.Set.of(new EngineModel(EngineModelName.valueOf("GEnx"),
                        Maker.valueOf("Boeing"), MotorizationType.TURBOFAN, FuelType.JET_A, 200000, 0.54))) {
            @Override
            public int maxPassengerCapacity() { return 300; }
        };

        final long smallId = modelRepo.saveAndGetId(smallModel);
        final long largeId = modelRepo.saveAndGetId(largeModel);
        seedAircraft("CS-TNA", smallId, COMPANY_TP, 100);
        seedAircraft("CS-TNB", largeId, COMPANY_TP, 200);

        final List<FleetEntry> result = newController().listFleet(null, null, 200);
        assertEquals(1, result.size());
        assertEquals("CS-TNB", result.get(0).aircraft().registration().value());
    }

    @Test
    public void ensureNoFilterMatchReturnsEmptyList() {
        final long modelId = modelRepo.saveAndGetId(buildModel("A320neo", "Airbus"));
        seedAircraft("CS-TNA", modelId, COMPANY_TP, 150);

        final List<FleetEntry> result = newController().listFleet("747", null, null);
        assertTrue(result.isEmpty());
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureListFleetRequiresCompanyCollaboratorRole() {
        AuthzTestHelper.loginAsPilot();
        newController().listFleet(null, null, null);
    }

    // ── Fake repositories ────────────────────────────────────────────────────

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

        @Override
        public Iterable<Aircraft> findAllByCompany(final CompanyIATACode companyCode) {
            return data.values().stream()
                    .filter(a -> a.companyCode().equals(companyCode))
                    .toList();
        }
    }

    private static class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();
        private long nextId = 1L;

        public long saveAndGetId(final AircraftModel entity) {
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
        public boolean containsOfIdentity(final Long id) {
            return data.containsKey(id);
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
        }

        @Override
        public void deleteOfIdentity(final Long id) {
            data.remove(id);
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
        public boolean containsOfIdentity(final Email id) {
            return data.containsKey(id);
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

        @Override
        public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
            return data.values().stream()
                    .filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode))
                    .toList();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
            return findAllActiveByCompanyCode(companyCode);
        }

        @Override
        public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
            return Collections.emptyList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
            return Collections.emptyList();
        }

        @Override
        public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
            return ofIdentity(email)
                    .filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c);
        }

        @Override
        public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
            return Optional.empty();
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
        public Optional<User> ofIdentity(final Email id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final Email id) {
            return data.containsKey(id);
        }

        @Override
        public Optional<User> findBySystemUser(final SystemUser systemUser) {
            return data.values().stream()
                    .filter(u -> u.systemUser().equals(systemUser))
                    .findFirst();
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
