package eapli.aisafe.app.atcc.console.presentation.flightroutemanagement;

import eapli.framework.actions.Action;

public class DeactivateFlightRouteAction implements Action {

    @Override
    public boolean execute() {
        return new DeactivateFlightRouteUI().show();
    }
}
