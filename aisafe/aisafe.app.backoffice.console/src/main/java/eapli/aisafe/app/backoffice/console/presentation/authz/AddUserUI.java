package eapli.aisafe.app.backoffice.console.presentation.authz;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

import eapli.aisafe.usermanagement.application.AddUserController;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AddUserUI extends AbstractUI {

	private final AddUserController theController = new AddUserController();

	@Override
	protected boolean doShow() {
		final String username = Console.readLine("Username");
		final String password = Console.readLine("Password");
		final String firstName = Console.readLine("First Name");
		final String lastName = Console.readLine("Last Name");
		final String email = Console.readLine("E-Mail");
		final String phoneNumber = Console.readLine("Phone Number");

		final Set<Role> roleTypes = new HashSet<>();
		boolean show;
		do {
			show = showRoles(roleTypes);
		} while (!show);

		if (roleTypes.isEmpty()) {
			System.out.println("At least one role must be selected.");
			return false;
		}

		try {
			final LocalDate clearanceExpiryDate = parseDate(Console.readLine("Security Clearance Expiry Date (yyyy-MM-dd)"),
					"Security Clearance Expiry Date");
			final LocalDate skillsAssessmentDate = parseDate(Console.readLine("Skills Assessment Date (yyyy-MM-dd)"),
					"Skills Assessment Date");

			theController.registerUser(username, password, firstName, lastName, email, phoneNumber, roleTypes,
					clearanceExpiryDate, skillsAssessmentDate);
			System.out.println("User successfully registered.");
		} catch (final IntegrityViolationException | ConcurrencyException e) {
			System.out.println("Registration failed: username or email is already in use.");
		} catch (final IllegalArgumentException e) {
			System.out.println("Validation error: " + e.getMessage());
		}

		return false;
	}

	private LocalDate parseDate(final String rawDate, final String fieldName) {
		try {
			return LocalDate.parse(rawDate);
		} catch (final DateTimeParseException e) {
			throw new IllegalArgumentException(fieldName + " must follow yyyy-MM-dd format");
		}
	}

	private boolean showRoles(final Set<Role> roleTypes) {
		final Menu rolesMenu = buildRolesMenu(roleTypes);
		final MenuRenderer renderer = new VerticalMenuRenderer(rolesMenu, MenuItemRenderer.DEFAULT);
		return renderer.render();
	}

	private Menu buildRolesMenu(final Set<Role> roleTypes) {
		final Menu rolesMenu = new Menu();
		int counter = 0;
		rolesMenu.addItem(MenuItem.of(counter++, "Done", Actions.SUCCESS));
		for (final Role roleType : theController.getRoleTypes()) {
			rolesMenu.addItem(MenuItem.of(counter++, roleType.toString(), () -> roleTypes.add(roleType)));
		}
		return rolesMenu;
	}

	@Override
	public String headline() {
		return "Register User";
	}
}
