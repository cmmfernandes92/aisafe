package eapli.aisafe.app.atcc.console.presentation.pilotmanagement;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilotmanagement.application.DeactivatePilotController;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class DeactivatePilotUI extends AbstractUI {

    private final DeactivatePilotController controller = new DeactivatePilotController(
            PersistenceContext.repositories().pilots(),
            PersistenceContext.repositories().flights(),
            PersistenceContext.repositories().aisafeUsers(),
            PersistenceContext.repositories().customerCollaborators());

    @Override
    protected boolean doShow() {
        System.out.println("\nActive Pilots:");
        boolean hasPilots = false;
        for (final Pilot pilot : controller.listActivePilotsOfMyCompany()) {
            System.out.println("  " + pilot.identity());
            hasPilots = true;
        }
        if (!hasPilots) {
            System.out.println("  No active pilots found.");
            return false;
        }

        final String pilotEmail = Console.readNonEmptyLine("Pilot email to deactivate",
                "Pilot email must not be blank.");

        try {
            controller.deactivatePilot(pilotEmail, new ConsoleAssignedFlightsReporter());
            System.out.println("Pilot deactivated: " + pilotEmail);
            return true;
        } catch (final IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Failed to deactivate pilot: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Deactivate Pilot";
    }
}