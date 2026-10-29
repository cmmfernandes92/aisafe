package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.Application;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaTransactionalRepository;

/* package */ class BaseJpaRepositoryBase<T, K, I> extends JpaTransactionalRepository<T, K, I> {

	BaseJpaRepositoryBase(final String persistenceUnitName, final String identityFieldName) {
		super(persistenceUnitName, Application.settings().getExtendedPersistenceProperties(), identityFieldName);
	}

	BaseJpaRepositoryBase(final String identityFieldName) {
		super(Application.settings().getPersistenceUnitName(),
				Application.settings().getExtendedPersistenceProperties(), identityFieldName);
	}
}
