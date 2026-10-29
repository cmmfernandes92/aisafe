package eapli.aisafe.infrastructure.authz;

import eapli.framework.infrastructure.authz.application.Authenticator;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;

public class AuthenticationCredentialHandler implements CredentialHandler {

	private final Authenticator authenticationService = AuthzRegistry.authenticationService();

	@Override
	public boolean authenticated(final String username, final String password, final Role... allowedRoles) {
		if (allowedRoles == null || allowedRoles.length == 0) {
			return authenticationService.authenticate(username, password).isPresent();
		}

		for (final Role role : allowedRoles) {
			if (authenticationService.authenticate(username, password, role).isPresent()) {
				return true;
			}
		}
		return false;
	}
}
