package eapli.aisafe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSettings {
	private static final Logger LOGGER = LoggerFactory.getLogger(AppSettings.class);

	private static final String PROPERTIES_RESOURCE = "application.properties";
	private static final String REPOSITORY_FACTORY_KEY = "persistence.repositoryFactory";
	private static final String UI_MENU_LAYOUT_KEY = "ui.menu.layout";
	private static final String PERSISTENCE_UNIT_KEY = "persistence.persistenceUnit";
	private static final String SCHEMA_GENERATION_KEY = "jakarta.persistence.schema-generation.database.action";

	private final Properties applicationProperties = new Properties();

	public AppSettings() {
		loadProperties();
	}

	private void loadProperties() {
		try (var propertiesStream = this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_RESOURCE)) {
			if (propertiesStream == null) {
				throw new FileNotFoundException(
						"property file '" + PROPERTIES_RESOURCE + "' not found in the classpath");
			}
			this.applicationProperties.load(propertiesStream);
		} catch (final IOException exio) {
			setDefaultProperties();
			LOGGER.warn("Loading default properties", exio);
		}
	}

	private void setDefaultProperties() {
		this.applicationProperties.setProperty(REPOSITORY_FACTORY_KEY,
				"eapli.aisafe.persistence.impl.jpa.JpaRepositoryFactory");
		this.applicationProperties.setProperty(UI_MENU_LAYOUT_KEY, "horizontal");
		this.applicationProperties.setProperty(PERSISTENCE_UNIT_KEY, "eapli.aisafe");
	}

	public Boolean isMenuLayoutHorizontal() {
		return "horizontal".equalsIgnoreCase(this.applicationProperties.getProperty(UI_MENU_LAYOUT_KEY));
	}

	public String getPersistenceUnitName() {
		return this.applicationProperties.getProperty(PERSISTENCE_UNIT_KEY);
	}

	public String getRepositoryFactory() {
		return this.applicationProperties.getProperty(REPOSITORY_FACTORY_KEY);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map getExtendedPersistenceProperties() {
		final Map ret = new HashMap();
		final String schemaGen = this.applicationProperties.getProperty(SCHEMA_GENERATION_KEY);
		if (schemaGen != null) {
			ret.put(SCHEMA_GENERATION_KEY, schemaGen);
		}
		return ret;
	}

	public String getProperty(final String prop) {
		return this.applicationProperties.getProperty(prop);
	}
}
