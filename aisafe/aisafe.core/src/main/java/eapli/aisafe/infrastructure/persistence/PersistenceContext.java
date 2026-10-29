package eapli.aisafe.infrastructure.persistence;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.Application;
import eapli.framework.util.Utility;

@Utility
public final class PersistenceContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceContext.class);

	private static RepositoryFactory theFactory;

	private PersistenceContext() {
	}

	public static RepositoryFactory repositories() {
		if (theFactory == null) {
			final String factoryClassName = Application.settings().getRepositoryFactory();
			try {
				theFactory = (RepositoryFactory) Class.forName(factoryClassName).getDeclaredConstructor().newInstance();
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException | ClassNotFoundException ex) {
				LOGGER.error("Unable to dynamically load the Repository Factory", ex);
				throw new IllegalStateException(
						"Unable to dynamically load the Repository Factory: " + factoryClassName, ex);
			}
		}
		return theFactory;
	}
}
