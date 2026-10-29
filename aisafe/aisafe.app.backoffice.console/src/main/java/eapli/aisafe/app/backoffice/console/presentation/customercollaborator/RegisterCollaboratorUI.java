package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import eapli.aisafe.customercollaboratormanagement.application.RegisterCollaboratorController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterCollaboratorUI extends AbstractUI {

	private static final int COMPANY_CUSTOMER = 1;
	private static final int AREA_CUSTOMER = 2;

	private final RegisterCollaboratorController controller = new RegisterCollaboratorController(
			PersistenceContext.repositories().customerCollaborators(),
			PersistenceContext.repositories().aisafeUsers(),
			PersistenceContext.repositories().airTransportCompanies(),
			PersistenceContext.repositories().airControlAreas());

	@Override
	protected boolean doShow() {
		System.out.println();
		System.out.println("REGISTER CUSTOMER'S COLLABORATOR");
		System.out.println();
		System.out.println("1. Air Transport Company");
		System.out.println("2. Air Control Area");
		final int customerType = Console.readInteger("Customer type");
		final String email = Console.readLine("User login email");
		final String position = Console.readLine("Position");

		try {
			if (customerType == COMPANY_CUSTOMER) {
				final String iataCode = Console.readLine("Company IATA code");
				controller.registerCompanyCollaborator(email, position, iataCode);
			} else if (customerType == AREA_CUSTOMER) {
				final String areaCode = Console.readLine("Area code");
				controller.registerAreaCollaborator(email, position, areaCode);
			} else {
				throw new IllegalArgumentException("Unknown customer type");
			}
			System.out.println();
			System.out.println("[ OK ] Customer collaborator registered successfully.");
		} catch (final IntegrityViolationException | ConcurrencyException e) {
			System.out.println();
			System.out.println("[FAIL] Collaborator already exists or could not be saved.");
		} catch (final IllegalArgumentException e) {
			System.out.println();
			System.out.println("[FAIL] " + e.getMessage());
		}
		return false;
	}

	@Override
	public String headline() {
		return "Register Customer's Collaborator";
	}
}
