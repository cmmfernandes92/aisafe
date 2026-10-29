package eapli.aisafe.usermanagement.application;
import java.time.LocalDate;

import java.util.Calendar;
import java.util.Set;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.EmailDomainPolicy;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.time.util.CurrentTimeCalendars;

@UseCaseController
public class AddUserController {

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final UserManagementService userSvc = AuthzRegistry.userService();
	private final eapli.framework.infrastructure.authz.domain.repositories.UserRepository authzUserRepo = PersistenceContext
			.repositories().users();
	private final eapli.aisafe.usermanagement.repositories.UserRepository userRepo = PersistenceContext.repositories()
			.aisafeUsers();

	public Role[] getRoleTypes() {
		return AisafeRoles.nonUserValues();
	}

	public SystemUser addUser(final String username, final String password, final String firstName,
			final String lastName, final String email, final Set<Role> roles, final Calendar createdOn) {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN);
		return userSvc.registerNewUser(username, password, firstName, lastName, email, roles, createdOn);
	}

	public SystemUser addUser(final String username, final String password, final String firstName,
			final String lastName, final String email, final Set<Role> roles) {
		return addUser(username, password, firstName, lastName, email, roles, CurrentTimeCalendars.now());
	}

	public User registerUser(final String username, final String password, final String firstName, final String lastName,
			final String email, final String phoneNumber, final Set<Role> roles, final LocalDate clearanceExpiryDate,
			final LocalDate skillsAssessmentDate) {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.ADMIN, AisafeRoles.OPERATOR);
		final Email userEmail = Email.valueOf(email);
		EmailDomainPolicy.ensureAllowedForRoles(userEmail, roles);
		if (userRepo.ofIdentity(userEmail).isPresent()) {
			throw new IllegalArgumentException(String.format("A user with email '%s' is already registered", userEmail));
		}
		final PhoneNumber validatedPhoneNumber = PhoneNumber.valueOf(phoneNumber);
		final SecurityClearance validatedSecurityClearance = SecurityClearance.valueOf(clearanceExpiryDate);
		final SkillsAssessment validatedSkillsAssessment = SkillsAssessment.valueOf(skillsAssessmentDate);

		final SystemUserBuilder userBuilder = UserBuilderHelper.builder();
		userBuilder.withUsername(username).withPassword(password).withName(firstName, lastName).withEmail(email)
				.withRoles(roles);
		final SystemUser systemUser = authzUserRepo.save(userBuilder.build());
		final User user = new User(systemUser, userEmail, validatedPhoneNumber, validatedSecurityClearance,
				validatedSkillsAssessment);
		return userRepo.save(user);
	}
}
