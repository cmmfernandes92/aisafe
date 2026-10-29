package eapli.aisafe.infrastructure.bootstrapers;
import java.time.LocalDate;

import java.util.HashSet;
import java.util.Set;

import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.domain.model.Role;

public class MasterUsersBootstrapper extends AbstractUserBootstrapper implements Action {

	@Override
	public boolean execute() {
		registerAdmin("admin", TestDataConstants.PASSWORD1, "Jane", "Doe Admin", TestDataConstants.ADMIN_EMAIL,
				TestDataConstants.ADMIN_PHONE, LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
		registerBackofficeOperator(TestDataConstants.BACKOFFICE_OPERATOR, TestDataConstants.PASSWORD1,
				"Bob", "Backoffice", TestDataConstants.BACKOFFICE_OPERATOR_EMAIL,
				TestDataConstants.BACKOFFICE_OPERATOR_PHONE, LocalDate.now().plusYears(2),
				LocalDate.now().minusMonths(6));
		return true;
	}

	private void registerAdmin(final String username, final String password, final String firstName,
			final String lastName, final String email, final String phoneNumber, final LocalDate clearanceExpiryDate,
			final LocalDate skillsAssessmentDate) {
		final Set<Role> roles = new HashSet<>();
		roles.add(AisafeRoles.ADMIN);
		registerUser(username, password, firstName, lastName, email, phoneNumber, roles, clearanceExpiryDate,
				skillsAssessmentDate);
	}

	private void registerBackofficeOperator(final String username, final String password, final String firstName,
			final String lastName, final String email, final String phoneNumber, final LocalDate clearanceExpiryDate,
			final LocalDate skillsAssessmentDate) {
		final Set<Role> roles = new HashSet<>();
		roles.add(AisafeRoles.BACKOFFICE_OPERATOR);
		registerUser(username, password, firstName, lastName, email, phoneNumber, roles, clearanceExpiryDate,
				skillsAssessmentDate);
	}
}
