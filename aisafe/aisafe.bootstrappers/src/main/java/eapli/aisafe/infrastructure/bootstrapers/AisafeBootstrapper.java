package eapli.aisafe.infrastructure.bootstrapers;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.strings.util.Strings;
import eapli.framework.validations.Invariants;

@SuppressWarnings("squid:S106")
public class AisafeBootstrapper implements Action {
	private static final Logger LOGGER = LoggerFactory.getLogger(AisafeBootstrapper.class);

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();
	private final UserRepository userRepository = PersistenceContext.repositories().users();
	private final eapli.aisafe.usermanagement.repositories.UserRepository aisafeUserRepository = PersistenceContext
			.repositories().aisafeUsers();

	@Override
	public boolean execute() {
		final Action[] actions = {
				new MasterUsersBootstrapper(),
				new CountryBootstrapper(),
				new MakerBootstrapper(),
				new EngineModelBootstrapper(),
				new AircraftModelBootstrapper(),
				new AirTransportCompanyBootstrapper(),
				new AirControlAreaBootstrapper(),
				new CollaboratorBootstrapper(),
				new AirportBootstrapper(),
				new AircraftBootstrapper(),
				new FlightRouteBootstrapper(),
				new PilotBootstrapper(),
				new FlightBootstrapper(),
				new WeatherPersonBootstrapper(),
				new SimulationRunBootstrapper(),
		};
		final SystemUser poweruser = registerPowerUser();
		if (poweruser == null) {
			return false;
		}
		authenticateForBootstrapping();
		registerPowerUserProfile(poweruser);

		var ret = true;
		for (final Action boot : actions) {
			if (boot instanceof CollaboratorBootstrapper) {
				authenticateAsBackofficeOperatorForBootstrapping();
			}
			System.out.println("Bootstrapping " + nameOfEntity(boot) + "...");
			ret &= boot.execute();
		}
		return ret;
	}

	private void registerPowerUserProfile(final SystemUser poweruser) {
		if (aisafeUserRepository.findBySystemUser(poweruser).isPresent()) {
			return;
		}

		try {
			final User userProfile = new User(poweruser, Email.valueOf(TestDataConstants.POWER_USER_EMAIL),
					PhoneNumber.valueOf(TestDataConstants.POWER_USER_PHONE),
					SecurityClearance.valueOf(LocalDate.now().plusYears(2)),
					SkillsAssessment.valueOf(LocalDate.now().minusMonths(6)));
			aisafeUserRepository.save(userProfile);
		} catch (ConcurrencyException | IntegrityViolationException e) {
			LOGGER.warn("Assuming {} profile already exists (activate trace log for details)", poweruser.username());
			LOGGER.trace("Assuming existing record", e);
		}
	}

	private SystemUser registerPowerUser() {
		final var userBuilder = UserBuilderHelper.builder();
		userBuilder.withUsername(TestDataConstants.POWER_USER).withPassword(TestDataConstants.POWER_USER_PASSWORD)
				.withName("joe", "power").withEmail(TestDataConstants.POWER_USER_EMAIL)
				.withRoles(AisafeRoles.POWER_USER, AisafeRoles.BACKOFFICE_OPERATOR,
						AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AisafeRoles.PILOT,
						AisafeRoles.WEATHER_PERSON);
		final var newUser = userBuilder.build();
		SystemUser poweruser = null;
		try {
			poweruser = userRepository.save(newUser);
		} catch (ConcurrencyException | IntegrityViolationException e) {
			LOGGER.warn("Assuming {} already exists (activate trace log for details)", newUser.username());
			LOGGER.trace("Assuming existing record", e);
			poweruser = userRepository.ofIdentity(Username.valueOf(TestDataConstants.POWER_USER)).orElse(null);
		}
		return poweruser;
	}

	protected void authenticateForBootstrapping() {
		authenticationService.authenticate(TestDataConstants.POWER_USER, TestDataConstants.POWER_USER_PASSWORD);
		Invariants.ensure(authz.hasSession());
	}

	private void authenticateAsBackofficeOperatorForBootstrapping() {
		authenticationService.authenticate(TestDataConstants.BACKOFFICE_OPERATOR, TestDataConstants.PASSWORD1,
				AisafeRoles.BACKOFFICE_OPERATOR);
		Invariants.ensure(authz.hasSession());
	}

	private String nameOfEntity(final Action boot) {
		final var name = boot.getClass().getSimpleName();
		return Strings.left(name, name.length() - "Bootstrapper".length());
	}
}
