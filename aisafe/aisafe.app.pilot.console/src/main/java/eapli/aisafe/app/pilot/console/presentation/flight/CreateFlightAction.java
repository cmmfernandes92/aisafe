package eapli.aisafe.app.pilot.console.presentation.flight;

import eapli.framework.actions.Action;

public class CreateFlightAction implements Action {

    @Override
    public boolean execute() {
        return new CreateFlightUI().show();
    }
}