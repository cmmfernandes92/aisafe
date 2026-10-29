package eapli.aisafe.app.atcc.console.presentation.flightroutemanagement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import eapli.aisafe.flightroutemanagement.application.DeactivateFlightRouteController;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class DeactivateFlightRouteUI extends AbstractUI {

    private final DeactivateFlightRouteController controller = new DeactivateFlightRouteController(
            PersistenceContext.repositories().flightRoutes(),
            PersistenceContext.repositories().flights(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        boolean any = false;
        System.out.println("Your company's active routes:");
        for (final FlightRoute route : controller.activeRoutesOfMyCompany()) {
            System.out.println("  " + route.routeName() + "  " + route);
            any = true;
        }
        if (!any) {
            System.out.println("  (none)");
            return false;
        }

        final String routeName = Console.readNonEmptyLine("Route name to deactivate (e.g. TP123)",
                "Route name must not be blank.");

        final LocalDate effectiveDate;
        try {
            effectiveDate = LocalDate.parse(
                    Console.readNonEmptyLine("Effective date (YYYY-MM-DD)",
                            "Date must not be blank."));
        } catch (final DateTimeParseException ex) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return false;
        }

        try {
            final FlightRoute route = controller.deactivateFlightRoute(routeName, effectiveDate);
            System.out.println("Flight route deactivated: " + route);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Failed to deactivate flight route: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Deactivate Flight Route";
    }
}
