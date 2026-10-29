package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.airtransportcompanymanagement.application.RegisterAirTransportCompanyController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class AirTransportCompanyBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirTransportCompanyBootstrapper.class);

    private final RegisterAirTransportCompanyController controller = new RegisterAirTransportCompanyController(
            PersistenceContext.repositories().airTransportCompanies());

    @Override
    public boolean execute() {
        register("TP", "TAP", "TAP Air Portugal");
        register("FR", "RYR", "Ryanair");
        register("LH", "DLH", "Lufthansa");
        return true;
    }

    private void register(final String iata, final String icao, final String name) {
        try {
            controller.register(iata, icao, name);
            LOGGER.debug("Registered air transport company: {} [{}/{}]", name, iata, icao);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming company '{}' already exists", name);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Failed to register company '{}': {}", name, e.getMessage());
        }
    }
}
