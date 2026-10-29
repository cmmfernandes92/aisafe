package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircraftmodelmanagement.application.AddEngineToAircraftModelController;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class AddEngineToAircraftModelUI extends AbstractUI {

    private final AddEngineToAircraftModelController controller = new AddEngineToAircraftModelController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels());

    @Override
    protected boolean doShow() {
        header();

        section("Step 1 — Select Aircraft Model");
        final List<AircraftModel> models = new ArrayList<>();
        controller.listAircraftModels().forEach(models::add);

        if (models.isEmpty()) {
            error("No aircraft models registered.");
            return false;
        }

        for (int i = 0; i < models.size(); i++) {
            System.out.printf("    %2d.  %s%n", i + 1, models.get(i));
        }
        final int modelChoice = Console.readInteger("  Select aircraft model (1-" + models.size() + ")");
        if (modelChoice < 1 || modelChoice > models.size()) {
            error("Invalid selection.");
            return false;
        }
        final AircraftModel selected = models.get(modelChoice - 1);
        System.out.println("  Selected: " + selected);

        section("Step 2 — Select Compatible Engine Model");
        final List<EngineModel> engines = new ArrayList<>();
        controller.listCompatibleEngines(selected.identity()).forEach(engines::add);

        if (engines.isEmpty()) {
            error("No compatible engine models available for this aircraft model.");
            return false;
        }

        for (int i = 0; i < engines.size(); i++) {
            System.out.printf("    %2d.  %s%n", i + 1, engines.get(i));
        }
        final int engineChoice = Console.readInteger("  Select engine model (1-" + engines.size() + ")");
        if (engineChoice < 1 || engineChoice > engines.size()) {
            error("Invalid selection.");
            return false;
        }
        final EngineModel engine = engines.get(engineChoice - 1);
        System.out.println("  Selected: " + engine);

        section("Confirm");
        System.out.printf("  Aircraft model  :  %s%n", selected);
        System.out.printf("  Engine model    :  %s%n", engine);
        System.out.println();

        final String confirm = Console.readLine("  Add this engine to the aircraft model? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Operation cancelled.\n");
            return false;
        }

        try {
            controller.addEngine(selected.identity(), engine.identity());
            ok("Engine model added to aircraft model successfully.");
        } catch (final IllegalArgumentException e) {
            error(e.getMessage());
        }
        return false;
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "ADD ENGINE MODEL TO AIRCRAFT MODEL");
        System.out.println("  └" + "─".repeat(50) + "┘");
    }

    private void section(final String title) {
        System.out.println();
        final String label = "── " + title + " ";
        System.out.println("  " + label + "─".repeat(Math.max(0, 50 - label.length())));
    }

    private void ok(final String msg) {
        System.out.println();
        System.out.println("  [ OK ]  " + msg);
        System.out.println();
    }

    private void error(final String msg) {
        System.out.println();
        System.out.println("  [FAIL]  " + msg);
        System.out.println();
    }

    @Override
    public String headline() {
        return "Add Engine Model to Aircraft Model";
    }
}
