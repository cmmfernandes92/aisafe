package eapli.aisafe.app.atcc.console.presentation.fleetmanagement;

import eapli.framework.actions.Action;

public class AddAircraftAction implements Action {

    @Override
    public boolean execute() {
        return new AddAircraftUI().show();
    }
}
