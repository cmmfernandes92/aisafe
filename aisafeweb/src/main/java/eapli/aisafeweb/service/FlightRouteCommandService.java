package eapli.aisafeweb.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import eapli.aisafe.flightroutemanagement.application.CreateFlightRouteController;
import eapli.aisafe.flightroutemanagement.application.DeactivateFlightRouteController;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class FlightRouteCommandService {

    private CreateFlightRouteController controller() {
        return new CreateFlightRouteController(
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers());
    }

    private DeactivateFlightRouteController deactivateController() {
        return new DeactivateFlightRouteController(
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers());
    }

    public FlightRoute create(final String routeName, final String originCode, final String destinationCode) {
        return controller().createFlightRoute(routeName, originCode, destinationCode);
    }

    public FlightRoute deactivate(final String routeName, final LocalDate effectiveDate) {
        return deactivateController().deactivateFlightRoute(routeName, effectiveDate);
    }
}
