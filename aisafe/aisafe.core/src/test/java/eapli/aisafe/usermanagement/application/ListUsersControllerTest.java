package eapli.aisafe.usermanagement.application;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;

public class ListUsersControllerTest {

	@BeforeClass
	public static void setUpAuthz() {
		AuthzTestHelper.configureAuthz();
	}

	@Before
	public void loginAsPowerUser() {
		AuthzTestHelper.loginAsPowerUser();
	}

	@After
	public void logout() {
		AuthzTestHelper.clearSession();
	}

	@Test
	public void ensureAllUsersReturnsRegisteredUsersForAuthorizedRole() {
		final ListUsersController controller = new ListUsersController();

		final List<SystemUser> users = toList(controller.allUsers());

		assertFalse(users.isEmpty());
		assertTrue(users.stream().anyMatch(u -> u.username().equals(Username.valueOf(AuthzTestHelper.ADMIN))));
		assertTrue(users.stream().anyMatch(u -> u.username().equals(Username.valueOf(AuthzTestHelper.POWER_USER))));
	}

	@Test(expected = UnauthorizedException.class)
	public void ensureAllUsersRequiresPowerUserOrAdmin() {
		AuthzTestHelper.loginAsPilot();
		final ListUsersController controller = new ListUsersController();

		controller.allUsers();
	}

	@Test
	public void ensureFindReturnsUserWhenUsernameExists() {
		final ListUsersController controller = new ListUsersController();

		final Optional<SystemUser> result = controller.find(Username.valueOf(AuthzTestHelper.ADMIN));

		assertTrue(result.isPresent());
	}

	@Test
	public void ensureFindReturnsEmptyWhenUsernameDoesNotExist() {
		final ListUsersController controller = new ListUsersController();

		final Optional<SystemUser> result = controller.find(Username.valueOf("missing-user"));

		assertFalse(result.isPresent());
	}

	private static <T> List<T> toList(final Iterable<T> iterable) {
		final List<T> result = new ArrayList<>();
		iterable.forEach(result::add);
		return result;
	}
}
