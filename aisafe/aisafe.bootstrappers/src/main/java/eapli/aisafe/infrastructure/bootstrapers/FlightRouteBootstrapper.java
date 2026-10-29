package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class FlightRouteBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlightRouteBootstrapper.class);

    @Override
    public boolean execute() {
        register("TP101", "TP", "LIS", "OPO");
        register("TP202", "TP", "LIS", "FNC");
        register("TP303", "TP", "OPO", "FNC");
        register("FR401", "FR", "OPO", "LIS");
        register("LH501", "LH", "LIS", "OPO");
        register("TP404", "TP", "FNC", "LIS");
        register("FR402", "FR", "LIS", "FNC");
        register("LH502", "LH", "OPO", "FNC");
        return true;
    }

    private void register(final String routeName, final String companyIata,
            final String originIata, final String destinationIata) {
        try {
            final var company = PersistenceContext.repositories().airTransportCompanies()
                    .ofIdentity(new CompanyIATACode(companyIata)).orElse(null);
            final var origin = PersistenceContext.repositories().airports()
                    .findByIataCode(new AirportIATACode(originIata)).orElse(null);
            final var destination = PersistenceContext.repositories().airports()
                    .findByIataCode(new AirportIATACode(destinationIata)).orElse(null);

            if (company == null || origin == null || destination == null) {
                LOGGER.warn("Skipping route {} — missing company or airport", routeName);
                return;
            }

            final var route = new FlightRoute(new RouteName(routeName), company, origin, destination);
            PersistenceContext.repositories().flightRoutes().save(route);
            LOGGER.debug("Registered flight route: {}", routeName);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming route '{}' already exists", routeName);
        } catch (final Exception e) {
            LOGGER.warn("Failed to register route '{}': {}", routeName, e.getMessage());
        }
    }
}
