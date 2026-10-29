package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.application.DisableCollaboratorController;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US064 — Disable a customer's collaborator.
 *
 * <p>After disabling: CollaboratorStatus = DISABLED and the underlying
 * SystemUser is deactivated — authentication is blocked (US064.AC2).
 */
public class DisableCollaboratorUI extends AbstractUI {

    private final DisableCollaboratorController controller = new DisableCollaboratorController(
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("DISABLE CUSTOMER'S COLLABORATOR");
        System.out.println();

        // Step 1 — filter by company IATA code
        final String iataInput = Console.readLine("Company IATA code");
        CompanyIATACode companyCode;
        try {
            companyCode = CompanyIATACode.valueOf(iataInput);
        } catch (final IllegalArgumentException e) {
            System.out.println("[FAIL] " + e.getMessage());
            return false;
        }

        // Step 2 — list active collaborators
        final List<CompanyCollaborator> list = new ArrayList<>();
        controller.activeCollaboratorsOf(companyCode).forEach(list::add);

        if (list.isEmpty()) {
            System.out.println("[FAIL] No active collaborators found for company " + iataInput.toUpperCase() + ".");
            return false;
        }

        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            final CompanyCollaborator c = list.get(i);
            System.out.printf("  [%d] %-30s | Position: %-20s | Contact: %s%n",
                    i + 1,
                    c.identity(),
                    c.position(),
                    c.contactEmail());
        }
        System.out.println();

        final int choice = Console.readInteger("Select collaborator number");
        if (choice < 1 || choice > list.size()) {
            System.out.println("[FAIL] Invalid selection.");
            return false;
        }

        final CompanyCollaborator selected = list.get(choice - 1);

        // Step 3 — confirm
        System.out.println();
        System.out.println("Login email  : " + selected.identity());
        System.out.println("Contact email: " + selected.contactEmail());
        System.out.println("Position     : " + selected.position());
        System.out.println("WARNING: This collaborator will be disabled and blocked from authenticating.");
        System.out.println();
        final String confirm = Console.readLine("Confirm disable? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("Operation cancelled.");
            return false;
        }

        // Step 4 — disable
        try {
            controller.disableCollaborator(selected.identity().toString());
            System.out.println();
            System.out.println("[ OK ] Collaborator " + selected.identity() + " has been disabled.");
        } catch (final IllegalStateException e) {
            System.out.println("[FAIL] " + e.getMessage());
        } catch (final ConcurrencyException e) {
            System.out.println("[FAIL] Operation failed due to a conflict — please try again.");
        } catch (final IllegalArgumentException e) {
            System.out.println("[FAIL] " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Disable Customer's Collaborator";
    }
}