package eapli.aisafe.app.backoffice.console.presentation.authz;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.usermanagement.application.ActivateUserController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

@SuppressWarnings("squid:S106")
public class ActivateUserUI extends AbstractUI {

    private final ActivateUserController controller = new ActivateUserController();

    @Override
    protected boolean doShow() {
        final List<SystemUser> users = toList(controller.deactivatedUsers());
        if (users.isEmpty()) {
            System.out.println("There are no disabled users to enable.");
            return false;
        }

        System.out.println("SELECT USER TO ENABLE");
        System.out.printf("%-6s%-15s%-25s%-25s%n", "Nº", "Username", "First Name", "Last Name");
        for (int i = 0; i < users.size(); i++) {
            final SystemUser user = users.get(i);
            System.out.printf("%-6d%-15s%-25s%-25s%n", i + 1, user.username(), user.name().firstName(),
                    user.name().lastName());
        }

        final int option = Console.readInteger("Enter user number to enable (0 to cancel)");
        if (option == 0) {
            System.out.println("Operation cancelled.");
            return false;
        }
        if (option < 1 || option > users.size()) {
            System.out.println("Invalid selection.");
            return false;
        }

        try {
            controller.activateUser(users.get(option - 1));
            System.out.println("User enabled successfully.");
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
        return "Enable User";
    }
}