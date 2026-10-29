package eapli.aisafe.usermanagement.application;

import java.util.Optional;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;

@UseCaseController
public class ListUsersController {

	public Iterable<SystemUser> allUsers() {
		authorizationService().ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
		return userService().allUsers();
	}

	public Optional<SystemUser> find(final Username u) {
		return userService().userOfIdentity(u);
	}

	protected AuthorizationService authorizationService() {
		return AuthzRegistry.authorizationService();
	}

	protected UserManagementService userService() {
		return AuthzRegistry.userService();
	}
}
