package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaUserRepository extends JpaAutoTxRepository<User, Email, Email>
		implements eapli.aisafe.usermanagement.repositories.UserRepository {

	public JpaUserRepository(final TransactionalContext autoTx) {
		super(autoTx, "email");
	}

	public JpaUserRepository(final String puname) {
		super(puname, Application.settings().getExtendedPersistenceProperties(), "email");
	}

	@Override
	public Optional<User> findBySystemUser(final SystemUser systemUser) {
		final Map<String, Object> params = new HashMap<>();
		params.put("systemUser", systemUser);
		return matchOne("e.systemUser=:systemUser", params);
	}
}
