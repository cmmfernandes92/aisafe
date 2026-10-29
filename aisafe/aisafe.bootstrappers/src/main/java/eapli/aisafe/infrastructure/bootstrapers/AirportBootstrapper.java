package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.airportmanagement.application.RegisterAirportController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;

@SuppressWarnings("squid:S106")
public class AirportBootstrapper implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(AirportBootstrapper.class);

    private final RegisterAirportController controller = new RegisterAirportController(
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().airControlAreas(),
            PersistenceContext.repositories().countries());

    @Override
    public boolean execute() {
        registerAirport("LPPT", "LIS", "Humberto Delgado", "Lisbon",
                114.0, 38.7742, -9.1342, "351", "LPPC");
        registerAirport("LPPR", "OPO", "Francisco Sa Carneiro", "Porto",
                69.0, 41.2356, -8.6781, "351", "LPPC");
        registerAirport("LPMA", "FNC", "Cristiano Ronaldo", "Funchal",
                58.0, 32.6979, -16.7745, "351", "LPPO");
        return true;
    }

    private void registerAirport(final String icaoCode, final String iataCode,
            final String airportName, final String town, final double elevationMeters,
            final double latitude, final double longitude, final String countryCode,
            final String areaCode) {
        try {
            controller.registerAirport(icaoCode, iataCode, airportName, town, elevationMeters,
                    latitude, longitude, countryCode, areaCode);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Assuming airport {} already registered (activate trace log for details)", icaoCode);
            LOGGER.trace("Assuming existing airport", e);
        }
    }
}
