package eapli.aisafe.app.atcc.console.presentation.pilotmanagement;

import eapli.framework.actions.Action;

public class AddPilotAction implements Action {

    @Override
    public boolean execute() {
        return new AddPilotUI().show();
    }
}
