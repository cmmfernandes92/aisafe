package eapli.aisafe.app.backoffice.console;

import eapli.aisafe.app.backoffice.console.presentation.MainMenu;
import eapli.aisafe.app.common.console.BaseApp;
import eapli.aisafe.app.common.console.presentation.authz.LoginUI;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;

@SuppressWarnings("squid:S106")
public final class BackofficeApp extends BaseApp {

	private BackofficeApp() {
	}

	public static void main(final String[] args) {
		AuthzRegistry.configure(PersistenceContext.repositories().users(), new AisafePasswordPolicy(),
				new PlainTextEncoder());
		new BackofficeApp().run(args);
	}

	@Override
	protected void doMain(final String[] args) {
		if (new LoginUI(new AuthenticationCredentialHandler(), AisafeRoles.POWER_USER, AisafeRoles.ADMIN,
				AisafeRoles.BACKOFFICE_OPERATOR).show()) {
			final var menu = new MainMenu();
			menu.mainLoop();
		}
	}

	@Override
	protected String appTitle() {
		return "AISafe Backoffice";
	}

	@Override
	protected String appGoodbye() {
		return "AISafe Backoffice";
	}

	@Override
	protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
		// no domain events to subscribe to in the base commit
	}
}
