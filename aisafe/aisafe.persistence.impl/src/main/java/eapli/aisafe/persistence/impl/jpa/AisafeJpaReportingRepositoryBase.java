package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.Application;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaTransactionalContext;

/* package */ class AisafeJpaReportingRepositoryBase extends JpaTransactionalContext {

	AisafeJpaReportingRepositoryBase() {
		super(Application.settings().getPersistenceUnitName(),
				Application.settings().getExtendedPersistenceProperties());
	}

	AisafeJpaReportingRepositoryBase(final String persistenceUnitName) {
		super(persistenceUnitName, Application.settings().getExtendedPersistenceProperties());
	}
}
