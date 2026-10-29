package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.customercollaboratormanagement.application.ListCollaboratorsController;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.User;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class ListCollaboratorsUI extends AbstractUI {

	private static final int COMPANY_CUSTOMER = 1;
	private static final int AREA_CUSTOMER = 2;

	private final ListCollaboratorsController controller = new ListCollaboratorsController(
			PersistenceContext.repositories().customerCollaborators(),
			PersistenceContext.repositories().aisafeUsers(),
			PersistenceContext.repositories().airTransportCompanies(),
			PersistenceContext.repositories().airControlAreas());

	@Override
	protected boolean doShow() {
		System.out.println();
		System.out.println("LIST CUSTOMER'S COLLABORATORS");
		System.out.println();
		System.out.println("1. Air Transport Company");
		System.out.println("2. Air Control Area");
		final int customerType = Console.readInteger("Customer type");

		try {
			if (customerType == COMPANY_CUSTOMER) {
				listCompanyCollaborators();
			} else if (customerType == AREA_CUSTOMER) {
				listAreaCollaborators();
			} else {
				throw new IllegalArgumentException("Unknown customer type");
			}
		} catch (final IllegalArgumentException e) {
			System.out.println();
			System.out.println("[FAIL] " + e.getMessage());
		}
		return false;
	}

	private void listCompanyCollaborators() {
		final List<AirTransportCompany> companies = toList(controller.allCompanies());
		if (companies.isEmpty()) {
			System.out.println();
			System.out.println("There are no air transport companies registered.");
			return;
		}

		System.out.println();
		for (int i = 0; i < companies.size(); i++) {
			final AirTransportCompany company = companies.get(i);
			System.out.printf("  [%d] %s%n", i + 1, company);
		}

		final int choice = Console.readInteger("Select company number");
		if (choice < 1 || choice > companies.size()) {
			throw new IllegalArgumentException("Invalid selection");
		}

		final AirTransportCompany company = companies.get(choice - 1);
		final List<CompanyCollaborator> collaborators = toList(controller.activeCollaboratorsOf(company.identity()));
		displayCollaborators(company.companyName(), collaborators);
	}

	private void listAreaCollaborators() {
		final List<AirControlArea> areas = toList(controller.allAreas());
		if (areas.isEmpty()) {
			System.out.println();
			System.out.println("There are no air control areas registered.");
			return;
		}

		System.out.println();
		for (int i = 0; i < areas.size(); i++) {
			final AirControlArea area = areas.get(i);
			System.out.printf("  [%d] %s [%s]%n", i + 1, area.name(), area.identity());
		}

		final int choice = Console.readInteger("Select area number");
		if (choice < 1 || choice > areas.size()) {
			throw new IllegalArgumentException("Invalid selection");
		}

		final AirControlArea area = areas.get(choice - 1);
		final List<AreaCollaborator> collaborators = toList(controller.activeCollaboratorsOf(area.identity()));
		displayCollaborators(area.name().toString(), collaborators);
	}

	private void displayCollaborators(final String customerName, final List<? extends Collaborator> collaborators) {
		System.out.println();
		if (collaborators.isEmpty()) {
			System.out.println(customerName + " has no active collaborators.");
			return;
		}

		System.out.printf("%-30s | %-35s | %-25s%n", "Name", "Login email", "Position");
		System.out.println("-----------------------------------------------------------------------------------------------");
		for (final Collaborator collaborator : collaborators) {
			System.out.printf("%-30s | %-35s | %-25s%n",
					userNameOf(collaborator),
					collaborator.identity(),
					collaborator.position());
		}
	}

	private String userNameOf(final Collaborator collaborator) {
		final Optional<User> user = controller.userOf(collaborator.identity());
		return user.map(u -> u.systemUser().name().toString()).orElse("<missing user>");
	}

	private static <T> List<T> toList(final Iterable<T> values) {
		final List<T> result = new ArrayList<>();
		values.forEach(result::add);
		return result;
	}

	@Override
	public String headline() {
		return "List Customer's Collaborators";
	}
}
