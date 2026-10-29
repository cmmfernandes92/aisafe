package eapli.aisafe.app.weather.console.presentation;

import eapli.framework.actions.Action;

public class ImportWeatherDataAction implements Action {

    @Override
    public boolean execute() {
        return new ImportWeatherDataUI().show();
    }
}