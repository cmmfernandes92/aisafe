package eapli.aisafe.usermanagement.application;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

@UseCaseController
public class ActivateUserController {

    public Iterable<SystemUser> deactivatedUsers() {
        authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
        return userService().deactivatedUsers();
    }

    public SystemUser activateUser(final SystemUser user) {
        authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
        if (user.isActive()) {
            throw new IllegalStateException("Cannot activate an active user");
        }
        return userService().activateUser(user);
    }

    protected AuthorizationService authorizationService() {
        return AuthzRegistry.authorizationService();
    }

    protected UserManagementService userService() {
        return AuthzRegistry.userService();
    }
}