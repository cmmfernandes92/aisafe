package eapli.aisafe.infrastructure.bootstrapers;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.customercollaboratormanagement.application.RegisterCollaboratorController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class CollaboratorBootstrapper extends AbstractUserBootstrapper implements Action {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollaboratorBootstrapper.class);

	private final RegisterCollaboratorController controller;

	public CollaboratorBootstrapper() {
		this(new RegisterCollaboratorController(PersistenceContext.repositories().customerCollaborators(),
				PersistenceContext.repositories().aisafeUsers(),
				PersistenceContext.repositories().airTransportCompanies(),
				PersistenceContext.repositories().airControlAreas()));
	}

	public CollaboratorBootstrapper(final RegisterCollaboratorController controller) {
		this.controller = controller;
	}

	@Override
	public boolean execute() {
		registerUser("tapcollab", TestDataConstants.PASSWORD1, "Ana", "Tavares",
				"ana.tavares@tap.pt", "+351910000100",
				Set.of(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR),
				LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
		registerUser("frcollab", TestDataConstants.PASSWORD1, "Miguel", "Santos",
				"miguel.santos@ryanair.pt", "+351910000102",
				Set.of(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR),
				LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
		registerUser("lhcollab", TestDataConstants.PASSWORD1, "Sophie", "Müller",
				"sophie.muller@lufthansa.pt", "+351910000103",
				Set.of(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR),
				LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));
		registerUser("lpccollab", TestDataConstants.PASSWORD1, "Luis", "Pinto",
				"luis.pinto@nav.pt", "+351910000101",
				Set.of(AisafeRoles.FLIGHT_CONTROL_OPERATOR),
				LocalDate.now().plusYears(2), LocalDate.now().minusMonths(6));

		registerCompanyCollaborator(TestDataConstants.POWER_USER_EMAIL, "Pilot", "TP");
		registerCompanyCollaborator("ana.tavares@tap.pt", "Operations Manager", "TP");
		registerCompanyCollaborator("miguel.santos@ryanair.pt", "Operations Manager", "FR");
		registerCompanyCollaborator("sophie.muller@lufthansa.pt", "Operations Manager", "LH");
		registerAreaCollaborator("luis.pinto@nav.pt", "FCO", "LPPC");
		return true;
	}

	private void registerCompanyCollaborator(final String email, final String position, final String companyCode) {
		try {
			controller.registerCompanyCollaborator(email, position, companyCode);
			LOGGER.debug("Registered company collaborator {} for {}", email, companyCode);
		} catch (final IntegrityViolationException | ConcurrencyException e) {
			LOGGER.debug("Assuming company collaborator '{}' already exists", email);
		} catch (final IllegalArgumentException e) {
			LOGGER.warn("Failed to register company collaborator '{}': {}", email, e.getMessage());
		}
	}

	private void registerAreaCollaborator(final String email, final String position, final String areaCode) {
		try {
			controller.registerAreaCollaborator(email, position, areaCode);
			LOGGER.debug("Registered area collaborator {} for {}", email, areaCode);
		} catch (final IntegrityViolationException | ConcurrencyException e) {
			LOGGER.debug("Assuming area collaborator '{}' already exists", email);
		} catch (final IllegalArgumentException e) {
			LOGGER.warn("Failed to register area collaborator '{}': {}", email, e.getMessage());
		}
	}
}
