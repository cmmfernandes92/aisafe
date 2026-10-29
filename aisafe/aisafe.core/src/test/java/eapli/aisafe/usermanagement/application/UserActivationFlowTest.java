package eapli.aisafe.usermanagement.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class UserActivationFlowTest {

    private static final String ADMIN_USERNAME = AuthzTestHelper.ADMIN;
    private static final String TARGET_USERNAME = "us032target";

    private DeactivateUserController deactivateController;
    private ActivateUserController activateController;
    private InMemoryUserRepository frameworkUsers;
    private AisafePasswordPolicy passwordPolicy;
    private PlainTextEncoder passwordEncoder;
    private SystemUser targetUser;

    @Before
    public void setUp() {
        passwordPolicy = new AisafePasswordPolicy();
        passwordEncoder = new PlainTextEncoder();
        frameworkUsers = new InMemoryUserRepository();

        frameworkUsers.save(buildSystemUser(ADMIN_USERNAME, AisafeRoles.ADMIN));
        targetUser = buildSystemUser(TARGET_USERNAME, AisafeRoles.BACKOFFICE_OPERATOR);
        frameworkUsers.save(targetUser);

        AuthzRegistry.configure(frameworkUsers, passwordPolicy, passwordEncoder);
        loginAsAdmin();

        deactivateController = new DeactivateUserController();
        activateController = new ActivateUserController();
    }

    @After
    public void tearDown() {
        AuthzRegistry.authorizationService().clearSession();
    }

    @Test
    public void ensureDisableBlocksAuthenticationAndRejectsSecondDisable() {
        final SystemUser disabled = deactivateController.deactivateUser(targetUser);
        assertFalse(disabled.isActive());

        AuthzRegistry.authorizationService().clearSession();
        final boolean authenticationSucceeded = AuthzRegistry.authenticationService()
                .authenticate(TARGET_USERNAME, AuthzTestHelper.PASSWORD, AisafeRoles.BACKOFFICE_OPERATOR).isPresent();
        assertFalse(authenticationSucceeded);

        loginAsAdmin();
        try {
            deactivateController.deactivateUser(disabled);
            fail("Disabling an already-disabled user should be rejected");
        } catch (final IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("inactive"));
        }
    }

    @Test
    public void ensureEnableWorksForDisabledUserAndRejectsAlreadyActiveUser() {
        final SystemUser disabled = deactivateController.deactivateUser(targetUser);
        assertFalse(disabled.isActive());

        final SystemUser enabled = activateController.activateUser(disabled);
        assertTrue(enabled.isActive());

        try {
            activateController.activateUser(enabled);
            fail("Enabling an active user should be rejected");
        } catch (final IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("active"));
        }
    }

    private void loginAsAdmin() {
        AuthzRegistry.authorizationService().clearSession();
        assertTrue(AuthzRegistry.authenticationService()
                .authenticate(ADMIN_USERNAME, AuthzTestHelper.PASSWORD, AisafeRoles.ADMIN).isPresent());
    }

    private SystemUser buildSystemUser(final String username, final Role role) {
        return new SystemUserBuilder(passwordPolicy, passwordEncoder)
                .withUsername(username)
                .withPassword(AuthzTestHelper.PASSWORD)
                .withName("Test", "User")
                .withEmail(username + "@aisafe.test")
                .withRoles(role)
                .build();
    }
}