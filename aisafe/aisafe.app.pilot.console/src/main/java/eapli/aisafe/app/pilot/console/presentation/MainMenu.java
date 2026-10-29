package eapli.aisafe.app.pilot.console.presentation;

import eapli.aisafe.Application;
import eapli.aisafe.app.common.console.presentation.authz.MyUserMenu;
import eapli.aisafe.app.common.console.presentation.weather.ConsultWeatherDataAction;
import eapli.aisafe.app.pilot.console.presentation.flight.CreateFlightAction;
import eapli.aisafe.app.pilot.console.presentation.flightplan.ImportFlightPlanAction;
import eapli.framework.actions.menu.Menu;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.HorizontalMenuRenderer;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;
import eapli.aisafe.app.pilot.console.presentation.flightplan.AttachWeatherToPlanAction;
import eapli.aisafe.app.pilot.console.presentation.flightplan.TestFlightPlanAction;

public class MainMenu extends AbstractUI {

	private static final int EXIT_OPTION = 0;
	private static final int MY_USER_OPTION = 1;
	private static final int CREATE_FLIGHT_OPTION = 2;
	private static final int IMPORT_FLIGHT_PLAN_OPTION = 3;
	private static final int CONSULT_WEATHER_DATA_OPTION = 4;
	private static final int ATTACH_WEATHER_OPTION = 5;
	private static final int TEST_FLIGHT_PLAN_OPTION = 6;

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
		return authz.session().map(s -> "Pilot [ @" + s.authenticatedUser().identity() + " ]")
				.orElse("Pilot [ ==Anonymous== ]");
	}

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final var myUserMenu = new MyUserMenu();
		mainMenu.addSubMenu(MY_USER_OPTION, myUserMenu);
		mainMenu.addItem(CREATE_FLIGHT_OPTION, "Create Flight",
				new CreateFlightAction());
		mainMenu.addItem(IMPORT_FLIGHT_PLAN_OPTION, "Import Flight Plan from File",
				new ImportFlightPlanAction());
		mainMenu.addItem(CONSULT_WEATHER_DATA_OPTION, "Consult Weather Data",
				new ConsultWeatherDataAction());
		mainMenu.addItem(ATTACH_WEATHER_OPTION, "Attach Weather to Flight Plan",
				new AttachWeatherToPlanAction());
		mainMenu.addItem(TEST_FLIGHT_PLAN_OPTION, "Test / Validate Flight Plan",
				new TestFlightPlanAction());
		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}
}
