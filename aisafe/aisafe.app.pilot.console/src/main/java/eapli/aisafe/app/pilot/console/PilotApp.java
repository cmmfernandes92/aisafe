package eapli.aisafe.app.pilot.console;

import eapli.aisafe.app.common.console.BaseApp;
import eapli.aisafe.app.common.console.presentation.authz.LoginUI;
import eapli.aisafe.app.pilot.console.infrastructure.tcp.PilotTcpServer;
import eapli.aisafe.app.pilot.console.presentation.MainMenu;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;


@SuppressWarnings("squid:S106")
public final class


PilotApp extends BaseApp {

	private PilotApp() {
	}

	public static void main(final String[] args) {
		AuthzRegistry.configure(PersistenceContext.repositories().users(), new AisafePasswordPolicy(),
				new PlainTextEncoder());
		new PilotApp().run(args);
	}

	@Override
	protected void doMain(final String[] args) {
		final Thread tcpThread = new Thread(new PilotTcpServer());
		tcpThread.setDaemon(true);
		tcpThread.start();

		if (new LoginUI(new AuthenticationCredentialHandler()).show()) {
			final var menu = new MainMenu();
			menu.mainLoop();
		}
	}

	@Override
	protected String appTitle() {
		return "AISafe Pilot";
	}

	@Override
	protected String appGoodbye() {
		return "AISafe Pilot";
	}

	@Override
	protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
		// no domain events to subscribe to in the base commit
	}
}
