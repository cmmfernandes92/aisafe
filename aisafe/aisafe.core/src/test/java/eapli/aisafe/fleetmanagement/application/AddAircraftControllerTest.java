package eapli.aisafe.fleetmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
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
import eapli.aisafe.fleetmanagement.domain.Aircraft;
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

public class AddAircraftControllerTest {

    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final CompanyIATACode COMPANY_TP = CompanyIATACode.valueOf("TP");

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void login() {
        AuthzTestHelper.loginAsPowerUser();
    }

    @After
    public void logout() {
        AuthzTestHelper.clearSession();
    }

    private final FakeAircraftRepository aircraftRepo = new FakeAircraftRepository();
    private final FakeCollaboratorRepository collaboratorRepo = new FakeCollaboratorRepository();
    private final FakeAircraftModelRepository modelRepo = new FakeAircraftModelRepository();
    private final FakeUserRepository userRepo = new FakeUserRepository();

    private AddAircraftController newController() {
        return new AddAircraftController(aircraftRepo, collaboratorRepo, modelRepo, userRepo);
    }

    private static AircraftModel buildModel() {
        final Maker maker = Maker.valueOf("Airbus");
        final EngineModel engine = new EngineModel(EngineModelName.valueOf("CFM56-5B"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), maker,
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                java.util.Set.of(engine));
    }

    private Map<SeatClass, Integer> seats(final int economy) {
        final Map<SeatClass, Integer> map = new EnumMap<>(SeatClass.class);
        map.put(SeatClass.ECONOMY, economy);
        return map;
    }

    @Test
    public void ensureAddAircraftForCompanyHappyPath() {
        final long modelId = modelRepo.saveAndGetId(buildModel());
        final Aircraft aircraft = newController()
                .addAircraftForCompany("CS-TNA", modelId, seats(150), "Portugal", "TP");

        assertNotNull(aircraft);
        assertEquals("CS-TNA", aircraft.registration().value());
        assertEquals(COMPANY_TP, aircraft.companyCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDuplicateRegistrationIsRejected() {
        final long modelId = modelRepo.saveAndGetId(buildModel());
        final AddAircraftController controller = newController();
        controller.addAircraftForCompany("CS-TNA", modelId, seats(150), "Portugal", "TP");
        controller.addAircraftForCompany("CS-TNA", modelId, seats(100), "Portugal", "TP");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureUnknownModelIsRejected() {
        newController().addAircraftForCompany("CS-TNA", 999L, seats(150), "Portugal", "TP");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureSeatCountExceedingCapacityIsRejected() {
        final Maker maker = Maker.valueOf("Airbus");
        final EngineModel engine = new EngineModel(EngineModelName.valueOf("CFM56-5B"), maker,
                MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 0.54);
        // Override maxPassengerCapacity() to simulate a model with a real cap of 180
        final AircraftModel capped = new AircraftModel(AircraftModelName.valueOf("A320capped"), maker,
                AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                42600, 79000, 64300, 26730, 12131, 833, 122.6, 0.024, 1.5, 6300,
                java.util.Set.of(engine)) {
            @Override
            public int maxPassengerCapacity() {
                return 180;
            }
        };
        final long modelId = modelRepo.saveAndGetId(capped);
        final Map<SeatClass, Integer> tooMany = new EnumMap<>(SeatClass.class);
        tooMany.put(SeatClass.ECONOMY, 200);
        tooMany.put(SeatClass.BUSINESS, 50);
        newController().addAircraftForCompany("CS-TNA", modelId, tooMany, "Portugal", "TP");
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureAddAircraftForCompanyRequiresPowerUserOrBackoffice() {
        AuthzTestHelper.loginAsPilot();
        newController().addAircraftForCompany("CS-TNA", 1L, seats(100), "Portugal", "TP");
    }

    @Test
    public void ensureAddAircraftHappyPathForAtccUser() {
        AuthzTestHelper.loginAsCompanyCollaborator();
        final SystemUser systemUser = AuthzRegistry.authorizationService()
                .session().get().authenticatedUser();
        userRepo.save(new User(systemUser, ATCC_EMAIL,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        collaboratorRepo.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Operations Manager"),
                COMPANY_TP, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));

        final long modelId = modelRepo.saveAndGetId(buildModel());
        final Aircraft aircraft = newController().addAircraft("CS-TNA", modelId, seats(150), "Portugal");

        assertNotNull(aircraft);
        assertEquals("CS-TNA", aircraft.registration().value());
        assertEquals(COMPANY_TP, aircraft.companyCode());
    }

    @Test
    public void ensureListModelsReturnsSeededModels() {
        modelRepo.saveAndGetId(buildModel());
        assertNotNull(newController().listModels().iterator().next());
    }

    @Test
    public void ensureAuthenticatedCompanyCodeResolvesFromSession() {
        AuthzTestHelper.loginAsCompanyCollaborator();
        final SystemUser systemUser = AuthzRegistry.authorizationService()
                .session().get().authenticatedUser();
        userRepo.save(new User(systemUser, ATCC_EMAIL,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
        collaboratorRepo.save(new CompanyCollaborator(ATCC_EMAIL, Position.valueOf("Operations Manager"),
                COMPANY_TP, ATCC_EMAIL, PhoneNumber.valueOf("+351910000000")));

        assertEquals(COMPANY_TP, newController().authenticatedCompanyCode());
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
        public Iterable<AircraftModel> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AircraftModel entity) { }

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
            return ofIdentity(email).filter(c -> c instanceof CompanyCollaborator)
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
