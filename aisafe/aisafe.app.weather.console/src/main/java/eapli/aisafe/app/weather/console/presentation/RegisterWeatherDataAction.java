package eapli.aisafe.app.weather.console.presentation;

import eapli.framework.actions.Action;

public class RegisterWeatherDataAction implements Action {

    @Override
    public boolean execute() {
        return new RegisterWeatherDataUI().show();
    }
}
