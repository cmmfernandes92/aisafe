package eapli.aisafe.usermanagement.repositories;

import java.util.Optional;

import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public interface UserRepository extends DomainRepository<Email, User> {

	Optional<User> findBySystemUser(SystemUser systemUser);
}
