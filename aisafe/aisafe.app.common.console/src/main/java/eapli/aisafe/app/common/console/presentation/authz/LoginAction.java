package eapli.aisafe.app.common.console.presentation.authz;

import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.domain.model.Role;

public class LoginAction implements Action {

	private final Role onlyWithThis;

	public LoginAction() {
		onlyWithThis = null;
	}

	public LoginAction(final Role onlyWithThis) {
		this.onlyWithThis = onlyWithThis;
	}

	@Override
	public boolean execute() {
		return new LoginUI(new AuthenticationCredentialHandler(), onlyWithThis).show();
	}
}
