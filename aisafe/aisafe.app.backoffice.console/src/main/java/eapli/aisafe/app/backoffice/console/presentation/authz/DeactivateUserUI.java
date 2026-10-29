package eapli.aisafe.app.backoffice.console.presentation.authz;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.usermanagement.application.DeactivateUserController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class DeactivateUserUI extends AbstractUI {

    private final DeactivateUserController controller = new DeactivateUserController();

    @Override
    protected boolean doShow() {
        final List<SystemUser> users = toList(controller.activeUsers());
        if (users.isEmpty()) {
            System.out.println("There are no active users to disable.");
            return false;
        }

        System.out.println("SELECT USER TO DISABLE");
        System.out.printf("%-6s%-15s%-25s%-25s%n", "Nº", "Username", "First Name", "Last Name");
        for (int i = 0; i < users.size(); i++) {
            final SystemUser user = users.get(i);
            System.out.printf("%-6d%-15s%-25s%-25s%n", i + 1, user.username(), user.name().firstName(),
                    user.name().lastName());
        }

        final int option = Console.readInteger("Enter user number to disable (0 to cancel)");
        if (option == 0) {
            System.out.println("Operation cancelled.");
            return false;
        }
        if (option < 1 || option > users.size()) {
            System.out.println("Invalid selection.");
            return false;
        }

        try {
            controller.deactivateUser(users.get(option - 1));
            System.out.println("User disabled successfully.");
        } catch (final IllegalStateException e) {
            System.out.println("Operation rejected: " + e.getMessage());
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("Operation failed due to a persistence conflict. Please try again.");
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
        return "Disable User";
    }
}