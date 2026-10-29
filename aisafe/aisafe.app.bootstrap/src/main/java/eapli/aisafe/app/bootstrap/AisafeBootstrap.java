package eapli.aisafe.app.bootstrap;

import eapli.aisafe.app.common.console.BaseApp;
import eapli.aisafe.infrastructure.bootstrapers.AisafeBootstrapper;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.infrastructure.smoketests.DemoSmokeTester;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.collections.util.ArrayPredicates;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.pubsub.EventDispatcher;

@SuppressWarnings("squid:S106")
public final class AisafeBootstrap extends BaseApp {

	private AisafeBootstrap() {
	}

	private boolean isToRunSampleE2E;

	public static void main(final String[] args) {
		AuthzRegistry.configure(PersistenceContext.repositories().users(), new AisafePasswordPolicy(),
				new PlainTextEncoder());
		new AisafeBootstrap().run(args);
	}

	@Override
	protected void doMain(final String[] args) {
		isToRunSampleE2E = ArrayPredicates.contains(args, "-smoke:basic");

		System.out.println("\n\n------- MASTER DATA -------");
		new AisafeBootstrapper().execute();

		if (isToRunSampleE2E) {
			System.out.println("\n\n------- BASIC SCENARIO -------");
			new DemoSmokeTester().execute();
		}
	}

	@Override
	protected String appTitle() {
		return "Bootstrapping data ";
	}

	@Override
	protected String appGoodbye() {
		return "Bootstrap data done.";
	}

	@Override
	protected void doSetupEventHandlers(final EventDispatcher dispatcher) {
		// no domain events to subscribe to in the base commit
	}
}
