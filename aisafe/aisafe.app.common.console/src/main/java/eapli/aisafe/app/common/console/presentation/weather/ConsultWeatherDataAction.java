package eapli.aisafe.app.common.console.presentation.weather;

import eapli.framework.actions.Action;

public class ConsultWeatherDataAction implements Action {

    @Override
    public boolean execute() {
        return new ConsultWeatherDataUI().show();
    }
}
