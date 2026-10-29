package eapli.aisafe.app.common.console.presentation.authz;

import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class LoginUI extends AbstractUI {

	private static final int DEFAULT_MAX_ATTEMPTS = 3;

	private final Role[] allowedRoles;
	private final int maxAttempts;
	private final CredentialHandler credentialHandler;

	public LoginUI(final CredentialHandler credentialHandler) {
		this(credentialHandler, new Role[0], DEFAULT_MAX_ATTEMPTS);
	}

	public LoginUI(final CredentialHandler credentialHandler, final Role onlyWithThis) {
		this(credentialHandler, new Role[] { onlyWithThis }, DEFAULT_MAX_ATTEMPTS);
	}

	public LoginUI(final CredentialHandler credentialHandler, final int maxAttempts) {
		this(credentialHandler, new Role[0], maxAttempts);
	}

	public LoginUI(final CredentialHandler credentialHandler, final Role[] allowedRoles, final int maxAttempts) {
		this.credentialHandler = credentialHandler;
		this.allowedRoles = allowedRoles == null ? new Role[0] : allowedRoles.clone();
		this.maxAttempts = maxAttempts;
	}

	public LoginUI(final CredentialHandler credentialHandler, final Role... allowedRoles) {
		this(credentialHandler, allowedRoles, DEFAULT_MAX_ATTEMPTS);
	}

	@Override
	protected boolean doShow() {
		var attempt = 1;
		while (attempt <= maxAttempts) {
			final String userName = Console.readNonEmptyLine("Username:", "Please provide a username");
			final String password = Console.readLine("Password:");

			if (credentialHandler.authenticated(userName, password, allowedRoles)) {
				return true;
			}
			System.out.printf("Invalid credentials or insufficient permissions. You have %d attempts left.%n%n»»»»»»»»»%n",
					maxAttempts - attempt);
			attempt++;
		}
		System.out.println("Authentication failed. Please contact your system administrator.");
		return false;
	}

	@Override
	public String headline() {
		return "Login";
	}
}
