package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.application.EditCollaboratorController;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US063 — Edit a customer's collaborator.
 *
 * <p>Editable: contactEmail and contactPhoneNumber (business contact details).
 * <p>Immutable: login email (system identity), position, customer, role.
 */
public class EditCollaboratorUI extends AbstractUI {

    private final EditCollaboratorController controller = new EditCollaboratorController(
            PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        System.out.println();
        System.out.println("EDIT CUSTOMER'S COLLABORATOR");
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

        // Step 2 — list active collaborators of that company
        final List<CompanyCollaborator> list = new ArrayList<>();
        controller.activeCollaboratorsOf(companyCode).forEach(list::add);

        if (list.isEmpty()) {
            System.out.println("[FAIL] No active collaborators found for company " + iataInput.toUpperCase() + ".");
            return false;
        }

        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            final CompanyCollaborator c = list.get(i);
            System.out.printf("  [%d] %-30s | Position: %-20s | Contact: %-30s | Phone: %s%n",
                    i + 1,
                    c.identity(),
                    c.position(),
                    c.contactEmail(),
                    c.contactPhoneNumber());
        }
        System.out.println();

        final int choice = Console.readInteger("Select collaborator number");
        if (choice < 1 || choice > list.size()) {
            System.out.println("[FAIL] Invalid selection.");
            return false;
        }

        final CompanyCollaborator selected = list.get(choice - 1);

        // Step 3 — collect new contact details
        System.out.println();
        System.out.println("Login email (identity, immutable): " + selected.identity());
        System.out.println("Current contact email            : " + selected.contactEmail());
        System.out.println("Current phone number             : " + selected.contactPhoneNumber());
        System.out.println();
        final String newContactEmail = Console.readLine("New contact email");
        final String newPhoneNumber  = Console.readLine("New phone number");

        // Step 4 — apply
        try {
            controller.editContactInfo(selected.identity().toString(), newContactEmail, newPhoneNumber);
            System.out.println();
            System.out.println("[ OK ] Collaborator contact details updated successfully.");
        } catch (final ConcurrencyException e) {
            System.out.println("[FAIL] Update failed due to a conflict — please try again.");
        } catch (final IllegalArgumentException e) {
            System.out.println("[FAIL] " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Edit Customer's Collaborator";
    }
}