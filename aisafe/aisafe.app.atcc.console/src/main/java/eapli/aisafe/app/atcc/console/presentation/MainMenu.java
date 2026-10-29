package eapli.aisafe.app.atcc.console.presentation;

import eapli.aisafe.Application;
import eapli.aisafe.app.atcc.console.presentation.fleetmanagement.AddAircraftAction;
import eapli.aisafe.app.atcc.console.presentation.fleetmanagement.DecommissionAircraftAction;
import eapli.aisafe.app.atcc.console.presentation.fleetmanagement.ListCompanyFleetAction;
import eapli.aisafe.app.atcc.console.presentation.flightroutemanagement.CreateFlightRouteAction;
import eapli.aisafe.app.atcc.console.presentation.flightroutemanagement.DeactivateFlightRouteAction;
import eapli.aisafe.app.atcc.console.presentation.pilotmanagement.AddPilotAction;
import eapli.aisafe.app.atcc.console.presentation.pilotmanagement.DeactivatePilotAction;
import eapli.aisafe.app.common.console.presentation.authz.MyUserMenu;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.actions.menu.Menu;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.HorizontalMenuRenderer;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {

	private static final int EXIT_OPTION = 0;
	private static final int MY_USER_OPTION = 1;
	private static final int ADD_AIRCRAFT_OPTION = 2;
	private static final int DECOMMISSION_AIRCRAFT_OPTION = 3;
	private static final int LIST_FLEET_OPTION = 4;
	private static final int CREATE_FLIGHT_ROUTE_OPTION = 5;
	private static final int ADD_PILOT_OPTION = 6;
	private static final int DEACTIVATE_FLIGHT_ROUTE_OPTION = 8;
	private static final int DEACTIVATE_PILOT_OPTION = 7;

	private final AuthorizationService authz = AuthzRegistry.authorizationService();

	@Override
	public boolean show() {
		drawFormTitle();
		return doShow();
	}

	@Override
	public boolean doShow() {
		final var menu = buildMainMenu();
		final MenuRenderer renderer;
		if (Application.settings().isMenuLayoutHorizontal()) {
			renderer = new HorizontalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
		} else {
			renderer = new VerticalMenuRenderer(menu, MenuItemRenderer.DEFAULT);
		}
		return renderer.render();
	}

	@Override
	public String headline() {
		return authz.session().map(s -> "ATCC [ @" + s.authenticatedUser().identity() + " ]")
				.orElse("ATCC [ ==Anonymous== ]");
	}

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final var myUserMenu = new MyUserMenu();
		mainMenu.addSubMenu(MY_USER_OPTION, myUserMenu);
		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR,
				AisafeRoles.POWER_USER)) {
			mainMenu.addItem(ADD_AIRCRAFT_OPTION, "Add Aircraft to Fleet", new AddAircraftAction());
		}
		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
			mainMenu.addItem(DECOMMISSION_AIRCRAFT_OPTION, "Decommission Aircraft", new DecommissionAircraftAction());
			mainMenu.addItem(LIST_FLEET_OPTION, "List Company Fleet", new ListCompanyFleetAction());
			mainMenu.addItem(CREATE_FLIGHT_ROUTE_OPTION, "Create Flight Route", new CreateFlightRouteAction());
			mainMenu.addItem(ADD_PILOT_OPTION, "Add Pilot", new AddPilotAction());
			mainMenu.addItem(DEACTIVATE_FLIGHT_ROUTE_OPTION, "Deactivate Flight Route",
					new DeactivateFlightRouteAction());
			mainMenu.addItem(DEACTIVATE_PILOT_OPTION, "Deactivate Pilot", new DeactivatePilotAction());
		}
		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}
}