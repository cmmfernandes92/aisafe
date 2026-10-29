package eapli.aisafe.customercollaboratormanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CollaboratorStatus;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
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
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class DisableCollaboratorControllerTest {

    private static final String COMPANY_CODE = "TP";
    private static final String COLLAB_EMAIL = "collab@test.pt";

    // These are re-created fresh before every test (NOT static)
    private FakeCollaboratorRepository collaboratorRepo;
    private FakeUserRepository userRepo;
    private DisableCollaboratorController controller;
    private SystemUser collaboratorSystemUser;

    /**
     * Runs before EVERY test — creates a completely fresh setup.
     */
    @Before
    public void setUp() {
        final AisafePasswordPolicy policy = new AisafePasswordPolicy();
        final PlainTextEncoder encoder    = new PlainTextEncoder();

        // Build a single framework repo containing ALL roles needed across tests
        final InMemoryUserRepository frameworkRepo = new InMemoryUserRepository();

        // Standard roles — required so AuthzTestHelper.loginAs*() can authenticate
        frameworkRepo.save(buildSystemUser(policy, encoder,
                AuthzTestHelper.BACKOFFICE, AisafeRoles.BACKOFFICE_OPERATOR));
        frameworkRepo.save(buildSystemUser(policy, encoder,
                AuthzTestHelper.POWER_USER, AisafeRoles.POWER_USER));
        frameworkRepo.save(buildSystemUser(policy, encoder,
                AuthzTestHelper.ADMIN, AisafeRoles.ADMIN));
        frameworkRepo.save(buildSystemUser(policy, encoder,
                AuthzTestHelper.PILOT, AisafeRoles.PILOT));

        // Fresh collaborator SystemUser for this test — starts ACTIVE every time
        collaboratorSystemUser = buildSystemUser(policy, encoder,
                "testcollab", AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        frameworkRepo.save(collaboratorSystemUser);

        // Configure AuthzRegistry with the combined repo
        AuthzRegistry.configure(frameworkRepo, policy, encoder);
        AuthzTestHelper.loginAsBackofficeOperator();

        // Fresh repositories and controller — no state bleeds between tests
        collaboratorRepo = new FakeCollaboratorRepository();
        userRepo         = new FakeUserRepository();
        controller       = new DisableCollaboratorController(collaboratorRepo, userRepo);
    }

    @After
    public void tearDown() {
        AuthzTestHelper.clearSession();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static SystemUser buildSystemUser(final AisafePasswordPolicy policy,
                                              final PlainTextEncoder encoder, final String username, final eapli.framework.infrastructure.authz.domain.model.Role role) {
        return new SystemUserBuilder(policy, encoder)
                .withUsername(username)
                .withPassword(AuthzTestHelper.PASSWORD)
                .withName("Test", "User")
                .withEmail(username + "@aisafe.test")
                .withRoles(role)
                .build();
    }

    private CompanyCollaborator givenActiveCollaborator() {
        final Email email = Email.valueOf(COLLAB_EMAIL);

        // AISafe User — needed by controller for userSvc.deactivateUser()
        userRepo.save(new User(
                collaboratorSystemUser,
                email,
                PhoneNumber.valueOf("+351910000000"),
                SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
                SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));

        // Collaborator
        final CompanyCollaborator c = new CompanyCollaborator(
                email, Position.valueOf("Officer"),
                CompanyIATACode.valueOf(COMPANY_CODE),
                email, PhoneNumber.valueOf("+351910000000"));
        collaboratorRepo.save(c);
        return c;
    }

    // -------------------------------------------------------------------------
    // activeCollaboratorsOf tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureActiveCollaboratorsOfFiltersCorrectlyByCompany() {
        givenActiveCollaborator();
        // Different company — must not appear
        final Email other = Email.valueOf("other@test.pt");
        collaboratorRepo.save(new CompanyCollaborator(
                other, Position.valueOf("Officer"), CompanyIATACode.valueOf("FR"),
                other, PhoneNumber.valueOf("+351910000001")));

        final List<CompanyCollaborator> result = StreamSupport.stream(
                controller.activeCollaboratorsOf(
                        CompanyIATACode.valueOf(COMPANY_CODE)).spliterator(), false).toList();

        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // disableCollaborator tests (US064)
    // -------------------------------------------------------------------------

    @Test
    public void ensureDisableCollaboratorSetsStatusToDisabled() {
        givenActiveCollaborator();

        controller.disableCollaborator(COLLAB_EMAIL);

        final Collaborator updated = collaboratorRepo.ofIdentity(
                Email.valueOf(COLLAB_EMAIL)).get();
        assertEquals(CollaboratorStatus.DISABLED, updated.status());
        assertFalse(updated.isActive());
    }

    @Test
    public void ensureDisabledCollaboratorNoLongerAppearsInActiveListings() {
        givenActiveCollaborator();

        controller.disableCollaborator(COLLAB_EMAIL);

        final List<CompanyCollaborator> active = StreamSupport.stream(
                controller.activeCollaboratorsOf(
                        CompanyIATACode.valueOf(COMPANY_CODE)).spliterator(), false).toList();
        assertTrue(active.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureDisableCollaboratorFailsWhenAlreadyDisabled() {
        final CompanyCollaborator c = givenActiveCollaborator();
        c.disable();
        collaboratorRepo.save(c);

        // Domain guard: throws if already DISABLED
        controller.disableCollaborator(COLLAB_EMAIL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDisableCollaboratorFailsWhenCollaboratorNotFound() {
        controller.disableCollaborator("nonexistent@test.pt");
    }

    // -------------------------------------------------------------------------
    // Authorization tests
    // -------------------------------------------------------------------------

    @Test(expected = UnauthorizedException.class)
    public void ensureDisableCollaboratorRequiresBackofficeOperator() {
        // PILOT is authenticated but not authorized → UnauthorizedException
        AuthzTestHelper.loginAsPilot();
        controller.disableCollaborator(COLLAB_EMAIL);
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureActiveCollaboratorsOfRequiresBackofficeOperator() {
        // PILOT is authenticated but not authorized → UnauthorizedException
        AuthzTestHelper.loginAsPilot();
        controller.activeCollaboratorsOf(CompanyIATACode.valueOf(COMPANY_CODE));
    }

    // -------------------------------------------------------------------------
    // Fake repositories
    // -------------------------------------------------------------------------

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

        @Override public Iterable<Collaborator> findAll()          { return data.values(); }
        @Override public long count()                              { return data.size(); }
        @Override public void delete(final Collaborator e)        { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final Email id)    { data.remove(id); }

        @Override
        public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(
                final CompanyIATACode companyCode) {
            return data.values().stream()
                    .filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode) && c.isActive())
                    .toList();
        }

        @Override
        public Iterable<CompanyCollaborator> findAllByCompanyCode(
                final CompanyIATACode companyCode) {
            return data.values().stream()
                    .filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c)
                    .filter(c -> c.companyCode().equals(companyCode))
                    .toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
            return data.values().stream()
                    .filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode) && c.isActive())
                    .toList();
        }

        @Override
        public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
            return data.values().stream()
                    .filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c)
                    .filter(c -> c.areaCode().equals(areaCode))
                    .toList();
        }

        @Override
        public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
            return ofIdentity(email)
                    .filter(c -> c instanceof CompanyCollaborator)
                    .map(c -> (CompanyCollaborator) c);
        }

        @Override
        public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
            return ofIdentity(email)
                    .filter(c -> c instanceof AreaCollaborator)
                    .map(c -> (AreaCollaborator) c);
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

        @Override public Iterable<User> findAll()                { return data.values(); }
        @Override public long count()                            { return data.size(); }
        @Override public void delete(final User e)              { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final Email id) { data.remove(id); }
    }
}