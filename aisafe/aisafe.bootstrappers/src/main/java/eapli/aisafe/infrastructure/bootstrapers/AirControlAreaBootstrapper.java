package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.aircontrolareamanagement.application.RegisterAirControlAreaController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;

@SuppressWarnings("squid:S106")
public class AirControlAreaBootstrapper implements Action {
	private static final Logger LOGGER = LoggerFactory.getLogger(AirControlAreaBootstrapper.class);

	private final RegisterAirControlAreaController controller = new RegisterAirControlAreaController(
			PersistenceContext.repositories().airControlAreas());

	@Override
	public boolean execute() {
		try {
			controller.registerArea("LPPO", "Santa Maria Oceanic FIR", 31.5, -17.5, 38.2, -13.001);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LPPO already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("LPPC", "Lisboa FIR", 36.5, -13.0, 42.5, -8.0);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LPPC already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("LECM", "Madrid FIR", 39.0, -7.999, 45.8, -1.501);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LECM already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("LFBB", "Bordeaux FIR", 43.5, -1.5, 47.5, 0.999);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LFBB already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("LFFF", "Paris FIR", 45.5, 1.0, 49.0, 3.499);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LFFF already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("LFEE", "Reims FIR", 47.0, 3.5, 50.3, 6.299);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area LFEE already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		try {
			controller.registerArea("EDGG", "Langen FIR", 48.3, 6.3, 50.9, 8.8);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Assuming area EDGG already registered (activate trace log for details)");
			LOGGER.trace("Assuming existing area", e);
		}
		return true;
	}
}
