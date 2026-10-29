package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircraftmodelmanagement.application.RemoveEngineModelFromAircraftModelController;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for US058 — Remove an engine model from an aircraft model.
 */
public class RemoveEngineModelFromAircraftModelUI extends AbstractUI {

    private final RemoveEngineModelFromAircraftModelController controller =
            new RemoveEngineModelFromAircraftModelController(
                    PersistenceContext.repositories().aircraftModels(),
                    PersistenceContext.repositories().engineModels(),
                    PersistenceContext.repositories().aircraft());

    @Override
    protected boolean doShow() {
        header();

        final AircraftModel model = selectAircraftModel();
        if (model == null) return false;

        final EngineModel engine = selectEngine(model);
        if (engine == null) return false;

        section("Summary");
        System.out.printf("  %-24s %s%n", "Aircraft model:", model);
        System.out.printf("  %-24s %s%n", "Engine to remove:", engine);
        System.out.println();

        final String confirm = Console.readLine("  Confirm removal? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Removal cancelled.\n");
            return false;
        }

        try {
            controller.removeEngine(model.identity(), engine.identity());
            ok("Engine \"" + engine + "\" removed from aircraft model \"" + model + "\".");
        } catch (final IllegalStateException e) {
            // Covers both A2 (last engine) and AC2 (active aircraft exist for this model)
            error(e.getMessage());
        } catch (final IllegalArgumentException e) {
            error("Validation failed: " + e.getMessage());
        } catch (final ConcurrencyException e) {
            error("Operation failed due to a conflict — please try again.");
        }
        return false;
    }

    private AircraftModel selectAircraftModel() {
        section("Select Aircraft Model");
        final List<AircraftModel> models = new ArrayList<>();
        controller.allAircraftModels().forEach(models::add);

        if (models.isEmpty()) {
            error("No aircraft models registered in the system.");
            return null;
        }

        for (int i = 0; i < models.size(); i++) {
            System.out.printf("  [%d] %s  (certified engines: %d)%n",
                    i + 1, models.get(i), models.get(i).certifiedEngines().size());
        }
        System.out.println();

        final int choice = Console.readInteger("  Select aircraft model number");
        if (choice < 1 || choice > models.size()) {
            error("Invalid selection.");
            return null;
        }
        return models.get(choice - 1);
    }

    private EngineModel selectEngine(final AircraftModel model) {
        section("Select Engine to Remove from " + model);

        final List<EngineModel> engines = new ArrayList<>();
        controller.certifiedEnginesOf(model).forEach(engines::add);

        if (engines.size() <= 1) {
            error("This aircraft model has only one certified engine and cannot have it removed (invariant A2).");
            return null;
        }

        for (int i = 0; i < engines.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, engines.get(i));
        }
        System.out.println();

        final int choice = Console.readInteger("  Select engine number");
        if (choice < 1 || choice > engines.size()) {
            error("Invalid selection.");
            return null;
        }
        return engines.get(choice - 1);
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "REMOVE ENGINE FROM AIRCRAFT MODEL");
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
        return "Remove Engine Model from Aircraft Model";
    }
}