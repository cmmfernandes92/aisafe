package eapli.aisafe.app.weather.console.presentation;

import eapli.aisafe.Application;
import eapli.aisafe.app.common.console.presentation.authz.MyUserMenu;
import eapli.aisafe.app.common.console.presentation.weather.ConsultWeatherDataAction;
import eapli.aisafe.app.weather.console.presentation.RegisterWeatherDataAction;
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
	private static final int REGISTER_WEATHER_DATA_OPTION = 2;
	private static final int IMPORT_WEATHER_DATA_OPTION = 3;
	private static final int CONSULT_WEATHER_DATA_OPTION = 4;

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
		return authz.session().map(s -> "Weather [ @" + s.authenticatedUser().identity() + " ]")
				.orElse("Weather [ ==Anonymous== ]");
	}

	private Menu buildMainMenu() {
		final var mainMenu = new Menu();
		final var myUserMenu = new MyUserMenu();
		mainMenu.addSubMenu(MY_USER_OPTION, myUserMenu);
		if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.WEATHER_PERSON)) {
			mainMenu.addItem(REGISTER_WEATHER_DATA_OPTION, "Register Weather Data",
					new RegisterWeatherDataAction());
			mainMenu.addItem(CONSULT_WEATHER_DATA_OPTION, "Consult Weather Data",
					new ConsultWeatherDataAction());
			mainMenu.addItem(IMPORT_WEATHER_DATA_OPTION, "Import Bulk Weather Data",
					new ImportWeatherDataAction());
		}
		mainMenu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Bye, Bye"));
		return mainMenu;
	}
}
