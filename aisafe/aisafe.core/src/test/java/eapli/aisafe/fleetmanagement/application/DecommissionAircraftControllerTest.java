package eapli.aisafe.fleetmanagement.application;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
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

public class DecommissionAircraftControllerTest {

    private static final Email ATCC_EMAIL = Email.valueOf(AuthzTestHelper.ATCC_USER + "@aisafe.test");
    private static final CompanyIATACode COMPANY_TP = CompanyIATACode.valueOf("TP");
    private static final CompanyIATACode COMPANY_BA = CompanyIATACode.valueOf("BA");

    private final FakeAircraftRepository aircraftRepo = new FakeAircraftRepository();
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

    private DecommissionAircraftController newController() {
        return new DecommissionAircraftController(aircraftRepo, collaboratorRepo, userRepo);
    }

    private Aircraft seedAircraft(final String reg, final CompanyIATACode company) {
        final Aircraft aircraft = new Aircraft(
                Registration.valueOf(reg),
                1L,
                company,
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 100)),
                new Country("Portugal"),
                0);
        return aircraftRepo.save(aircraft);
    }

    @Test
    public void ensureDecommissionHappyPath() {
        seedAircraft("CS-TNA", COMPANY_TP);
        final Aircraft result = newController().decommissionAircraft("CS-TNA");
        assertEquals(OperationalStatus.RETIRED, result.status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWrongCompanyIsRejected() {
        seedAircraft("CS-TNA", COMPANY_BA);
        newController().decommissionAircraft("CS-TNA");
    }

    @Test(expected = IllegalStateException.class)
    public void ensureAlreadyRetiredIsRejected() {
        final Aircraft aircraft = seedAircraft("CS-TNA", COMPANY_TP);
        aircraft.decommission();
        newController().decommissionAircraft("CS-TNA");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureUnknownRegistrationIsRejected() {
        newController().decommissionAircraft("CS-ZZZ");
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureDecommissionRequiresCompanyCollaboratorRole() {
        AuthzTestHelper.loginAsPilot();
        seedAircraft("CS-TNA", COMPANY_TP);
        newController().decommissionAircraft("CS-TNA");
    }

    @Test
    public void ensureListCompanyAircraftReturnsOnlyActive() {
        seedAircraft("CS-TNA", COMPANY_TP);
        final Aircraft retired = seedAircraft("CS-TNB", COMPANY_TP);
        retired.decommission();

        int count = 0;
        for (final Aircraft a : newController().listCompanyAircraft()) {
            assertEquals(OperationalStatus.ACTIVE, a.status());
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void ensureListCompanyAircraftExcludesOtherCompanies() {
        seedAircraft("CS-TNA", COMPANY_TP);
        seedAircraft("G-ABCD", COMPANY_BA);

        int count = 0;
        for (final Aircraft a : newController().listCompanyAircraft()) {
            assertEquals(COMPANY_TP, a.companyCode());
            count++;
        }
        assertEquals(1, count);
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
