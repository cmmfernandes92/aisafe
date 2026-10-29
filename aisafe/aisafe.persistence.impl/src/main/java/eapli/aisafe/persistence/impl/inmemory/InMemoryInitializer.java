package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.infrastructure.bootstrapers.AisafeBootstrapper;

final class InMemoryInitializer {

	private static class LazyHolder {
		private static final InMemoryInitializer INSTANCE = new InMemoryInitializer();

		private LazyHolder() {
		}
	}

	private boolean initialized;

	private InMemoryInitializer() {
	}

	private synchronized void initialize() {
		if (!initialized) {
			initialized = true;
			try {
				new AisafeBootstrapper().execute();
			} catch (RuntimeException ex) {
				initialized = false;
				throw ex;
			}
		}
	}

	public static void init() {
		LazyHolder.INSTANCE.initialize();
	}
}
