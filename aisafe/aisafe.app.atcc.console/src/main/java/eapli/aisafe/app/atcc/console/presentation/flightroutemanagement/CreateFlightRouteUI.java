package eapli.aisafe.app.atcc.console.presentation.flightroutemanagement;

import eapli.aisafe.flightroutemanagement.application.CreateFlightRouteController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class CreateFlightRouteUI extends AbstractUI {

    private final CreateFlightRouteController controller = new CreateFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().airports(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        final String routeName = Console.readNonEmptyLine("Route name (e.g. TP123)",
                "Route name must not be blank.");
        final String originCode = Console.readNonEmptyLine("Origin airport code (IATA or ICAO)",
                "Origin airport code must not be blank.");
        final String destinationCode = Console.readNonEmptyLine("Destination airport code (IATA or ICAO)",
                "Destination airport code must not be blank.");

        try {
            final var route = controller.createFlightRoute(routeName, originCode, destinationCode);
            System.out.println("Flight route created: " + route);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Failed to create flight route: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Create Flight Route";
    }
}
