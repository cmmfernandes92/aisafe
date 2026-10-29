package eapli.aisafe.app.backoffice.console.presentation.authz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eapli.aisafe.usermanagement.application.DeactivateUserController;
import eapli.aisafe.usermanagement.application.ListUsersController;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public class ListUsersUITest {

	private PrintStream originalOut;
	private ByteArrayOutputStream outputStream;

	@Before
	public void redirectStdOut() {
		originalOut = System.out;
		outputStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outputStream));
	}

	@After
	public void restoreStdOut() {
		System.setOut(originalOut);
	}

	@Test
	public void ensureDoShowDisplaysMessageWhenThereAreNoUsers() {
		final TestableListUsersUI ui = new TestableListUsersUI(
				new FakeListUsersController(List.of()),
				new FakeDeactivateUserController(List.of()));

		final boolean result = ui.runDoShow();
		final String output = outputStream.toString();

		assertFalse(result);
		assertTrue(output.contains("No users registered."));
	}

	@Test
	public void ensureDoShowDisplaysStatusesForActiveAndInactiveUsers() {
		final SystemUser activeUser = user("active-user", "Active", "User");
		final SystemUser inactiveUser = user("inactive-user", "Inactive", "User");
		final TestableListUsersUI ui = new TestableListUsersUI(
				new FakeListUsersController(List.of(activeUser, inactiveUser)),
				new FakeDeactivateUserController(List.of(activeUser)));

		final boolean result = ui.runDoShow();
		final String output = outputStream.toString();

		assertFalse(result);
		assertTrue(output.contains("USERNAME"));
		assertTrue(output.contains("STATUS"));
		assertTrue(output.contains("active-user"));
		assertTrue(output.contains("inactive-user"));
		assertTrue(output.contains("ACTIVE"));
		assertTrue(output.contains("INACTIVE"));
	}

	@Test
	public void ensureHeadlineMatchesUseCaseName() {
		final TestableListUsersUI ui = new TestableListUsersUI(
				new FakeListUsersController(List.of()),
				new FakeDeactivateUserController(List.of()));

		assertEquals("List Users", ui.headline());
	}

	private static SystemUser user(final String username, final String firstName, final String lastName) {
		return UserBuilderHelper.builder()
				.withUsername(username)
				.withPassword("Password1")
				.withName(firstName, lastName)
				.withEmail(username + "@aisafe.pt")
				.withRoles(AisafeRoles.ADMIN)
				.build();
	}

	private static class TestableListUsersUI extends ListUsersUI {

		TestableListUsersUI(final ListUsersController listUsersController,
				final DeactivateUserController deactivateUserController) {
			super(listUsersController, deactivateUserController);
		}

		boolean runDoShow() {
			return super.doShow();
		}
	}

	private static class FakeListUsersController extends ListUsersController {
		private final Iterable<SystemUser> users;

		FakeListUsersController(final Iterable<SystemUser> users) {
			this.users = users;
		}

		@Override
		public Iterable<SystemUser> allUsers() {
			return users;
		}
	}

	private static class FakeDeactivateUserController extends DeactivateUserController {
		private final Iterable<SystemUser> activeUsers;

		FakeDeactivateUserController(final Iterable<SystemUser> activeUsers) {
			this.activeUsers = activeUsers;
		}

		@Override
		public Iterable<SystemUser> activeUsers() {
			return activeUsers;
		}
	}
}
