package eapli.aisafe.infrastructure.bootstrapers;
import java.time.LocalDate;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;

public class AbstractUserBootstrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserBootstrapper.class);

	final AddUserController userController = new AddUserController();

	public AbstractUserBootstrapper() {
		super();
	}

	protected User registerUser(final String username, final String password, final String firstName,
			final String lastName, final String email, final String phoneNumber, final Set<Role> roles,
			final LocalDate clearanceExpiryDate, final LocalDate skillsAssessmentDate) {
		User u = null;
		try {
			u = userController.registerUser(username, password, firstName, lastName, email, phoneNumber, roles,
					clearanceExpiryDate, skillsAssessmentDate);
			LOGGER.debug("»»» {}", username);
		} catch (final IntegrityViolationException | ConcurrencyException e) {
			LOGGER.debug("Assuming {} already exists (activate trace log for details)", username);
			LOGGER.trace("Assuming existing record", e);
		} catch (final IllegalArgumentException e) {
			LOGGER.warn("Skipping invalid bootstrap user '{}': {}", username, e.getMessage());
		}
		return u;
	}
}
