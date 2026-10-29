package eapli.aisafe.app.atcc.console.presentation.pilotmanagement;

import java.util.LinkedHashSet;
import java.util.Set;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilotmanagement.application.AddPilotController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class AddPilotUI extends AbstractUI {

    private final AddPilotController controller = new AddPilotController(
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().aisafeUsers(),
            PersistenceContext.repositories().airTransportCompanies(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        System.out.println("\nAvailable Aircraft Models:");
        for (final AircraftModel model : controller.listAircraftModels()) {
            System.out.printf("  [%d] %s%n", model.identity(), model);
        }

        final String pilotEmail = Console.readNonEmptyLine("Pilot email", "Pilot email must not be blank.");
        final String modelIdsRaw = Console.readNonEmptyLine("Certified aircraft model IDs (comma-separated)",
                "At least one aircraft model id is required.");
        final Set<Long> modelIds = parseModelIds(modelIdsRaw);

        try {
            final var pilot = controller.addPilot(pilotEmail, modelIds);
            System.out.println("Pilot added: " + pilot.identity());
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Failed to add pilot: " + ex.getMessage());
            return false;
        }
    }

    private Set<Long> parseModelIds(final String raw) {
        final Set<Long> result = new LinkedHashSet<>();
        for (final String token : raw.split(",")) {
            final String trimmed = token.strip();
            if (!trimmed.isEmpty()) {
                try {
                    result.add(Long.parseLong(trimmed));
                } catch (final NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid aircraft model id: " + trimmed);
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("At least one aircraft model id is required");
        }
        return result;
    }

    @Override
    public String headline() {
        return "Add Pilot";
    }
}
