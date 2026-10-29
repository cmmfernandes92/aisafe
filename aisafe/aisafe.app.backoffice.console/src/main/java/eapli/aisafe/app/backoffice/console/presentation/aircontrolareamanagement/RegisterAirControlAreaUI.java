package eapli.aisafe.app.backoffice.console.presentation.aircontrolareamanagement;

import eapli.aisafe.aircontrolareamanagement.application.RegisterAirControlAreaController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAirControlAreaUI extends AbstractUI {

	private final RegisterAirControlAreaController controller =
			new RegisterAirControlAreaController(
					PersistenceContext.repositories().airControlAreas());

	@Override
	protected boolean doShow() {
		final String code = Console.readNonEmptyLine("Area Code", "Area code must not be blank.");
		final String name = Console.readNonEmptyLine("Area Name", "Area name must not be blank.");

		try {
			final double swLat = parseDouble(Console.readLine("Southwest Latitude"), "Southwest Latitude");
			final double swLon = parseDouble(Console.readLine("Southwest Longitude"), "Southwest Longitude");
			final double neLat = parseDouble(Console.readLine("Northeast Latitude"), "Northeast Latitude");
			final double neLon = parseDouble(Console.readLine("Northeast Longitude"), "Northeast Longitude");

			controller.registerArea(code, name, swLat, swLon, neLat, neLon);
			System.out.println("Air control area registered: " + code + " - " + name);
			return true;
		} catch (final IllegalArgumentException ex) {
			System.out.println("Registration failed: " + ex.getMessage());
			return false;
		}
	}

	private double parseDouble(final String rawValue, final String fieldName) {
		try {
			return Double.parseDouble(rawValue);
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + " must be a valid number");
		}
	}

	@Override
	public String headline() {
		return "Register Air Control Area";
	}
}