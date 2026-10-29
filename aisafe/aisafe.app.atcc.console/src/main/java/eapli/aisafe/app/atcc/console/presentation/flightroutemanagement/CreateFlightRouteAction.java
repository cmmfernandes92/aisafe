package eapli.aisafe.app.atcc.console.presentation.flightroutemanagement;

import eapli.framework.actions.Action;

public class CreateFlightRouteAction implements Action {

    @Override
    public boolean execute() {
        return new CreateFlightRouteUI().show();
    }
}
