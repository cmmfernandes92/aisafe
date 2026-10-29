package eapli.aisafe.app.pilot.console.presentation.flightplan;

import eapli.framework.actions.Action;

public class AttachWeatherToPlanAction implements Action {

    @Override
    public boolean execute() {
        new AttachWeatherToPlanUI().show();
        return false;
    }
}