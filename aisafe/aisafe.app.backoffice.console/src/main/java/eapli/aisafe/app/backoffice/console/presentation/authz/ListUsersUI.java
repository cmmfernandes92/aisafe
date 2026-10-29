package eapli.aisafe.app.backoffice.console.presentation.authz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eapli.aisafe.usermanagement.application.DeactivateUserController;
import eapli.aisafe.usermanagement.application.ListUsersController;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class ListUsersUI extends AbstractUI {

	private final ListUsersController listUsersController;
	private final DeactivateUserController deactivateUserController;
	public ListUsersUI() {
		this(new ListUsersController(), new DeactivateUserController());
	}

	ListUsersUI(final ListUsersController listUsersController,
			final DeactivateUserController deactivateUserController) {
		this.listUsersController = listUsersController;
		this.deactivateUserController = deactivateUserController;
	}

	@Override
	protected boolean doShow() {
		final List<SystemUser> allUsers = toList(listUsersController.allUsers());
		if (allUsers.isEmpty()) {
			System.out.println("No users registered.");
			return false;
		}

		final Set<Username> activeUsers = new HashSet<>();
		for (final SystemUser activeUser : deactivateUserController.activeUsers()) {
			activeUsers.add(activeUser.username());
		}

		System.out.printf("%-15s%-25s%-25s%-10s%n", "USERNAME", "FIRST NAME", "LAST NAME", "STATUS");
		for (final SystemUser user : allUsers) {
			final String status = activeUsers.contains(user.username()) ? "ACTIVE" : "INACTIVE";
			System.out.printf("%-15s%-25s%-25s%-10s%n", user.username(), user.name().firstName(),
					user.name().lastName(), status);
		}
		return false;
	}

	private static <T> List<T> toList(final Iterable<T> values) {
		final List<T> result = new ArrayList<>();
		values.forEach(result::add);
		return result;
	}

	@Override
	public String headline() {
		return "List Users";
	}
}
