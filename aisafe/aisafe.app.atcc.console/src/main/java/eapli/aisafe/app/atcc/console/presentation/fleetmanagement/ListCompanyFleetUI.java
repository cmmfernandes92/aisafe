package eapli.aisafe.app.atcc.console.presentation.fleetmanagement;

import java.util.List;

import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController;
import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController.FleetEntry;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class ListCompanyFleetUI extends AbstractUI {

    private final ListCompanyFleetController controller = new ListCompanyFleetController(
            PersistenceContext.repositories().aircraft(),
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().customerCollaborators(),
            PersistenceContext.repositories().aisafeUsers());

    @Override
    protected boolean doShow() {
        System.out.println("\nOptional filters (press Enter to skip each):");
        final String modelNameFilter = Console.readLine("  Model name contains");
        final String makerFilter = Console.readLine("  Maker contains");
        final Integer minCapacity = readOptionalPositiveInt("  Min passenger capacity");

        final List<FleetEntry> fleet = controller.listFleet(modelNameFilter, makerFilter, minCapacity);

        if (fleet.isEmpty()) {
            System.out.println("\nNo aircraft found.");
            return true;
        }

        System.out.printf("%n%-10s  %-18s  %-10s  %5s  %8s  %5s  %s%n",
                "Reg.", "Model", "Maker", "Econ.", "Business", "First", "Status");
        System.out.println("-".repeat(78));
        for (final FleetEntry entry : fleet) {
            final var cabin = entry.aircraft().cabinConfiguration();
            System.out.printf("%-10s  %-18s  %-10s  %5d  %8d  %5d  %s%n",
                    entry.aircraft().registration().value(),
                    entry.model().modelName(),
                    entry.model().maker(),
                    cabin.economySeats(),
                    cabin.businessSeats(),
                    cabin.firstClassSeats(),
                    entry.aircraft().status());
        }
        System.out.printf("%nTotal: %d aircraft%n", fleet.size());
        return true;
    }

    private Integer readOptionalPositiveInt(final String prompt) {
        final String raw = Console.readLine(prompt);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            final int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (final NumberFormatException e) {
            System.out.println("  Invalid number — filter ignored.");
            return null;
        }
    }

    @Override
    public String headline() {
        return "Company Fleet";
    }
}
