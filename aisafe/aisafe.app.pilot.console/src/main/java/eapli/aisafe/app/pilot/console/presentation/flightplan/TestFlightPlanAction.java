package eapli.aisafe.app.pilot.console.presentation.flightplan;

import eapli.framework.actions.Action;

public class TestFlightPlanAction implements Action {

    @Override
    public boolean execute() {
        return new TestFlightPlanUI().show();
    }
}
