package eapli.aisafe.app.atcc.console.presentation.pilotmanagement;

import eapli.framework.actions.Action;

public class DeactivatePilotAction implements Action {

    @Override
    public boolean execute() {
        return new DeactivatePilotUI().show();
    }
}