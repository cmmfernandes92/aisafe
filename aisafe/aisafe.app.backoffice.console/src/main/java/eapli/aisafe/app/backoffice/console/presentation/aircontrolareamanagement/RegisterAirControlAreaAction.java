package eapli.aisafe.app.backoffice.console.presentation.aircontrolareamanagement;

import eapli.framework.actions.Action;

public class RegisterAirControlAreaAction implements Action {

	@Override
	public boolean execute() {
		return new RegisterAirControlAreaUI().show();
	}
}