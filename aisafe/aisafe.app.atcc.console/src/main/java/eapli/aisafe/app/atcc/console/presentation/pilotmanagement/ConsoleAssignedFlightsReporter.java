package eapli.aisafe.app.atcc.console.presentation.pilotmanagement;

import eapli.aisafe.pilotmanagement.application.AssignedFlightDTO;
import eapli.aisafe.pilotmanagement.application.AssignedFlightsReporter;

public class ConsoleAssignedFlightsReporter implements AssignedFlightsReporter {

    @Override
    public void report(final Iterable<AssignedFlightDTO> flights) {
        boolean hasFlights = false;
        for (final AssignedFlightDTO dto : flights) {
            if (!hasFlights) {
                System.out.println("\nAssigned flights for this pilot:");
                hasFlights = true;
            }
            System.out.printf("  Route: %-8s | Departure: %s | Aircraft: %s%n",
                    dto.routeName(), dto.departureDate(), dto.aircraftRegistration());
        }
        if (!hasFlights) {
            System.out.println("\nNo assigned flights found for this pilot.");
        }
    }
}