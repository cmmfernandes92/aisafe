package eapli.aisafe.app.atcc.console.presentation.fleetmanagement;

import java.util.EnumMap;
import java.util.Map;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.fleetmanagement.application.AddAircraftController;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class AddAircraftUI extends AbstractUI {

    private final AddAircraftController controller = new AddAircraftController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        final Iterable<AircraftModel> models = controller.listModels();

        System.out.println("\nAvailable Aircraft Models:");
        for (final AircraftModel model : models) {
            System.out.printf("  [%d] %s%n", model.identity(), model);
        }

        final String modelIdStr = Console.readNonEmptyLine("Model ID", "Model ID must not be blank.");
        final Long modelId;
        try {
            modelId = Long.parseLong(modelIdStr.trim());
        } catch (final NumberFormatException e) {
            System.out.println("Invalid model ID.");
            return false;
        }

        final String registration = Console.readNonEmptyLine("Aircraft Registration (e.g. CS-TNA)",
                "Registration must not be blank.");

        final int economySeats = readNonNegativeInt("Economy seats (0 if none)");
        final int businessSeats = readNonNegativeInt("Business seats (0 if none)");
        final int firstSeats = readNonNegativeInt("First class seats (0 if none)");

        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, economySeats);
        seats.put(SeatClass.BUSINESS, businessSeats);
        seats.put(SeatClass.FIRST, firstSeats);

        final String country = Console.readNonEmptyLine("Registration Country", "Country must not be blank.");

        try {
            final var aircraft = controller.addAircraft(registration, modelId, seats, country);
            System.out.println("Aircraft registered: " + aircraft);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Registration failed: " + ex.getMessage());
            return false;
        }
    }

    private int readNonNegativeInt(final String prompt) {
        while (true) {
            final String raw = Console.readLine(prompt);
            try {
                final int value = Integer.parseInt(raw.trim());
                if (value >= 0) {
                    return value;
                }
                System.out.println("Value must be 0 or greater.");
            } catch (final NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    @Override
    public String headline() {
        return "Add Aircraft to Fleet";
    }
}
