package eapli.aisafe.app.backoffice.console.presentation.customercollaborator;

import eapli.framework.actions.Action;

public class ListPilotAction implements Action {

    @Override
    public boolean execute() {
        return new ListPilotUI().show();
    }
}