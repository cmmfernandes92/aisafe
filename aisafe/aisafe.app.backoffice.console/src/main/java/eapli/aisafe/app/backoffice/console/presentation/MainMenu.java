package eapli.aisafe.app.backoffice.console.presentation;

import eapli.aisafe.Application;
import eapli.aisafe.app.backoffice.console.presentation.aircontrolareamanagement.RegisterAirControlAreaAction;
import eapli.aisafe.app.backoffice.console.presentation.airportmanagement.RegisterAirportAction;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.AddEngineToAircraftModelUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.RegisterAircraftModelUI;
import eapli.aisafe.app.backoffice.console.presentation.aircraftmodel.RemoveEngineModelFromAircraftModelUI;
import eapli.aisafe.app.backoffice.console.presentation.airtransportcompany.RegisterAirTransportCompanyUI;
import eapli.aisafe.app.backoffice.console.presentation.authz.AddUserUI;
import eapli.aisafe.app.backoffice.console.presentation.authz.ActivateUserUI;
import eapli.aisafe.app.backoffice.console.presentation.authz.DeactivateUserUI;
import eapli.aisafe.app.backoffice.console.presentation.authz.ListUsersUI;
import eapli.aisafe.app.backoffice.console.presentation.customercollaborator.DisableCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.customercollaborator.EditCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.customercollaborator.ListCollaboratorsUI;
import eapli.aisafe.app.backoffice.console.presentation.customercollaborator.RegisterCollaboratorUI;
import eapli.aisafe.app.backoffice.console.presentation.enginemodel.RegisterEngineModelUI;
import eapli.aisafe.app.common.console.presentation.authz.MyUserMenu;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.aisafe.app.backoffice.console.presentation.customercollaborator.ListPilotAction;

public class MainMenu extends AbstractUI {
	private static final String RETURN_LABEL = "Return ";
	private static final String SEPARATOR_LABEL = "--------------";

	private static final int EXIT_OPTION = 0;
	private static final int MY_USER_OPTION = 1;
	private static final int USERS_OPTION = 2;
	private static final int AIRCRAFT_OPTION = 3;
	private static final int COMPANY_OPTION = 4;
	private static final int AIR_CONTROL_AREAS_OPTION = 5;
	private static final int AIRPORTS_OPTION = 6;
	private static final int CUSTOMER_COLLABORATORS_OPTION = 7;

	private static final int ADD_USER_OPTION = 1;
	private static final int LIST_USERS_OPTION = 2;
	private static final int DEACTIVATE_USER_OPTION = 3;
	private static final int ACTIVATE_USER_OPTION = 4;

	// Aircraft sub-menu
	private static final int REGISTER_ENGINE_MODEL_OPTION = 1;
	private static final int REGISTER_AIRCRAFT_MODEL_OPTION = 2;
	private static final int ADD_ENGINE_TO_AIRCRAFT_OPTION = 3;
	private static final int REMOVE_ENGINE_FROM_AIRCRAFT_OPTION = 4;  // US058

	// Company sub-menu
	private static final int REGISTER_COMPANY_OPTION = 1;

	// Customer Collaborators sub-menu
	private static final int REGISTER_COLLABORATOR_OPTION = 1;
	private static final int LIST_COLLABORATORS_OPTION = 2;   // US062
	private static final int EDIT_COLLABORATOR_OPTION = 3;    // US063
	private static final int DISABLE_COLLABORATOR_OPTION = 4; // US064

	private static final int LIST_PILOTS_OPTION = 5;

	private static final int REGISTER_AREA_OPTION = 1;
	private static final int REGISTER_AIRPORT_OPTION = 1;

	private final AuthorizationService authz = AuthzRegistry.authorizationService();

	@Override
	public boolean show() {
		drawFormTitle();
		return doShow();
	}

	@Override
	public boolean doShow() {
		final var menu = buildMainMenu();
		return new PrettyMenuRenderer(menu, MenuItemRenderer.DEFAULT, "Main Menu").render();
	}

	@Override
	public String headline() {
		return authz.session().map(s -> "Backoffice [ @" + s.authenticatedUser().identity() + " ]")
				.orElse("Backoffice [ ==Anonymous== ]");
	}

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final var myUserMenu = new MyUserMenu();
		mainMenu.addSubMenu(MY_USER_OPTION, myUserMenu);

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}

		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.POWER_USER, AisafeRoles.ADMIN, AisafeRoles.OPERATOR)) {
			mainMenu.addSubMenu(USERS_OPTION, buildUsersMenu());
			mainMenu.addSubMenu(AIRCRAFT_OPTION, buildAircraftMenu());
			mainMenu.addSubMenu(COMPANY_OPTION, buildCompanyMenu());
		}
		
		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.BACKOFFICE_OPERATOR,
				AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
			mainMenu.addSubMenu(CUSTOMER_COLLABORATORS_OPTION, buildCustomerCollaboratorsMenu());
		}


		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.POWER_USER, AisafeRoles.BACKOFFICE_OPERATOR)) {
			mainMenu.addSubMenu(AIR_CONTROL_AREAS_OPTION, buildAirControlAreasMenu());
			mainMenu.addSubMenu(AIRPORTS_OPTION, buildAirportsMenu());
		}

		if (!Application.settings().isMenuLayoutHorizontal()) {
			mainMenu.addItem(MenuItem.separator(SEPARATOR_LABEL));
		}
		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}

	private Menu buildUsersMenu() {
		final var usersMenu = new Menu("Users >");
		usersMenu.addItem(ADD_USER_OPTION, "Register User", new AddUserUI()::show);
		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.POWER_USER, AisafeRoles.ADMIN)) {
			usersMenu.addItem(LIST_USERS_OPTION, "List Users", new ListUsersUI()::show);
			usersMenu.addItem(DEACTIVATE_USER_OPTION, "Disable User", new DeactivateUserUI()::show);
			usersMenu.addItem(ACTIVATE_USER_OPTION, "Enable User", new ActivateUserUI()::show);
		}
		usersMenu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return usersMenu;
	}

	private Menu buildAircraftMenu() {
		final var menu = new Menu("Aircraft >");
		menu.addItem(REGISTER_ENGINE_MODEL_OPTION,       "Register Engine Model",             new RegisterEngineModelUI()::show);
		menu.addItem(REGISTER_AIRCRAFT_MODEL_OPTION,     "Register Aircraft Model",           new RegisterAircraftModelUI()::show);
		menu.addItem(ADD_ENGINE_TO_AIRCRAFT_OPTION,      "Add Engine to Aircraft Model",      new AddEngineToAircraftModelUI()::show);
		menu.addItem(REMOVE_ENGINE_FROM_AIRCRAFT_OPTION, "Remove Engine from Aircraft Model", new RemoveEngineModelFromAircraftModelUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCompanyMenu() {
		final var menu = new Menu("Companies >");
		menu.addItem(REGISTER_COMPANY_OPTION, "Register Air Transport Company", new RegisterAirTransportCompanyUI()::show);
		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildCustomerCollaboratorsMenu() {
		final var menu = new Menu("Customer Collaborators >");
		menu.addItem(REGISTER_COLLABORATOR_OPTION, "Register Customer's Collaborator", new RegisterCollaboratorUI()::show);
		menu.addItem(LIST_COLLABORATORS_OPTION,    "List Customer's Collaborators",    new ListCollaboratorsUI()::show);
		menu.addItem(EDIT_COLLABORATOR_OPTION,     "Edit Customer's Collaborator",     new EditCollaboratorUI()::show);
		menu.addItem(DISABLE_COLLABORATOR_OPTION,  "Disable Customer's Collaborator",  new DisableCollaboratorUI()::show);
		menu.addItem(LIST_PILOTS_OPTION, "List Pilot Roster", new ListPilotAction());

		menu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return menu;
	}

	private Menu buildAirControlAreasMenu() {
		final var areasMenu = new Menu("Air Control Areas >");
		areasMenu.addItem(REGISTER_AREA_OPTION, "Register Air Control Area",
				new RegisterAirControlAreaAction());
		areasMenu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return areasMenu;
	}

	private Menu buildAirportsMenu() {
		final var airportsMenu = new Menu("Airports >");
		airportsMenu.addItem(REGISTER_AIRPORT_OPTION, "Register Airport",
				new RegisterAirportAction());
		airportsMenu.addItem(EXIT_OPTION, RETURN_LABEL, Actions.SUCCESS);
		return airportsMenu;
	}
}
