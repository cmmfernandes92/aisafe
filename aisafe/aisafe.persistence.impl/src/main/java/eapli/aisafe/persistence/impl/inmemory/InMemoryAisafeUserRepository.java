package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAisafeUserRepository extends InMemoryDomainRepository<User, Email>
		implements eapli.aisafe.usermanagement.repositories.UserRepository {

	static {
		InMemoryInitializer.init();
	}

	@Override
	public Optional<User> findBySystemUser(final SystemUser systemUser) {
		return matchOne(e -> e.systemUser().equals(systemUser));
	}
}
