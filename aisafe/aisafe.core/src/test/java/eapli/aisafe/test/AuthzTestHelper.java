package eapli.aisafe.test;

import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public final class AuthzTestHelper {

	public static final String BACKOFFICE = "backoffice";
	public static final String POWER_USER = "poweruser";
	public static final String ADMIN = "admin";
	public static final String PILOT = "pilot";
	public static final String ATCC_USER = "atcc";
	public static final String WEATHER_PERSON_USER = "weatherperson";
	public static final String FCO_USER = "flightcontroloperator";
	public static final String PASSWORD = "Password1";

	private AuthzTestHelper() {
	}

	public static void configureAuthz() {
		final var userRepository = new InMemoryUserRepository();
		seedUser(userRepository, BACKOFFICE, AisafeRoles.BACKOFFICE_OPERATOR);
		seedUser(userRepository, POWER_USER, AisafeRoles.POWER_USER);
		seedUser(userRepository, ADMIN, AisafeRoles.ADMIN);
		seedUser(userRepository, PILOT, AisafeRoles.PILOT);
		seedUser(userRepository, ATCC_USER, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		seedUser(userRepository, WEATHER_PERSON_USER, AisafeRoles.WEATHER_PERSON);
		seedUser(userRepository, FCO_USER, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		AuthzRegistry.configure(userRepository, new AisafePasswordPolicy(), new PlainTextEncoder());
	}

	public static void loginAsBackofficeOperator() {
		loginAs(BACKOFFICE, AisafeRoles.BACKOFFICE_OPERATOR);
	}

	public static void loginAsPowerUser() {
		loginAs(POWER_USER, AisafeRoles.POWER_USER);
	}

	public static void loginAsAdmin() {
		loginAs(ADMIN, AisafeRoles.ADMIN);
	}

	public static void loginAsPilot() {
		loginAs(PILOT, AisafeRoles.PILOT);
	}

	public static void loginAsCompanyCollaborator() {
		loginAs(ATCC_USER, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
	}

	public static void loginAsWeatherPerson() {
		loginAs(WEATHER_PERSON_USER, AisafeRoles.WEATHER_PERSON);
	}

	public static void loginAsFlightControlOperator() {
		loginAs(FCO_USER, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
	}

	public static void clearSession() {
		AuthzRegistry.authorizationService().clearSession();
	}

	private static void loginAs(final String username, final Role role) {
		clearSession();
		AuthzRegistry.authenticationService().authenticate(username, PASSWORD, role);
	}

	private static void seedUser(final InMemoryUserRepository repository, final String username, final Role role) {
		final var userBuilder = new SystemUserBuilder(new AisafePasswordPolicy(), new PlainTextEncoder());
		repository.save(userBuilder
				.withUsername(username)
				.withPassword(PASSWORD)
				.withName("Test", "User")
				.withEmail(username + "@aisafe.test")
				.withRoles(role)
				.build());
	}
}
