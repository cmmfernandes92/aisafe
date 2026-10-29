package eapli.aisafe.usermanagement.application;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

@UseCaseController
public class DeactivateUserController {

	public Iterable<SystemUser> activeUsers() {
		authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
		return userService().activeUsers();
	}

	public SystemUser deactivateUser(final SystemUser user) {
		authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
		return userService().deactivateUser(user);
	}

	protected AuthorizationService authorizationService() {
		return AuthzRegistry.authorizationService();
	}

	protected UserManagementService userService() {
		return AuthzRegistry.userService();
	}
}
