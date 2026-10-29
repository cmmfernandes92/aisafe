package eapli.aisafe.app.backoffice.console.presentation.aircraftmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eapli.aisafe.aircraftmodelmanagement.application.RegisterAircraftModelController;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterAircraftModelUI extends AbstractUI {

    private final RegisterAircraftModelController controller = new RegisterAircraftModelController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels(),
            PersistenceContext.repositories().makers());

    @Override
    protected boolean doShow() {
        header();

        section("Identification");
        final String modelName = Console.readLine("  Aircraft model name");
        System.out.println();
        final List<eapli.aisafe.makermanagement.domain.Maker> makers = new ArrayList<>();
        controller.listMakers().forEach(makers::add);
        if (makers.isEmpty()) {
            error("No manufacturers registered. Please register a maker first.");
            return false;
        }
        System.out.println("  Available manufacturers:");
        for (int i = 0; i < makers.size(); i++) {
            System.out.printf("    %2d.  %s%n", i + 1, makers.get(i));
        }
        final String makerName = Console.readLine("  Manufacturer name");

        section("Classification");
        final AircraftType type = readEnum("  Aircraft type", AircraftType.values());
        System.out.println();
        final MotorizationType motorizationType = readEnum("  Motorization type", MotorizationType.values());

        section("Weight & Fuel");
        final double emptyWeight    = Console.readDouble("  Empty weight (kg)       ");
        final double mtow           = Console.readDouble("  MTOW (kg)               ");
        final double mzfw           = Console.readDouble("  MZFW (kg)               ");
        final double maxFuelCapacity = Console.readDouble("  Max fuel capacity (L)   ");

        section("Performance");
        final double serviceCeiling = Console.readDouble("  Service ceiling (ft)    ");
        final double cruiseSpeed    = Console.readDouble("  Cruise speed (km/h)     ");
        final double maximumRange   = Console.readDouble("  Maximum range (km)      ");

        section("Aerodynamics");
        final double wingArea          = Console.readDouble("  Wing area (m²)          ");
        final double wingSpan          = Console.readDouble("  Wing span (m)           ");
        final double dragCoefficient   = Console.readDouble("  Drag coefficient (Cd0)  ");
        final double liftCoefficient   = Console.readDouble("  Lift coefficient (Cl)   ");

        section("Certified Engine Models");
        final Set<EngineModel> certifiedEngines = selectEngines(motorizationType);
        if (certifiedEngines.isEmpty()) {
            error("At least one certified engine model is required.");
            return false;
        }

        section("Summary");
        System.out.printf("  %-26s %s%n",  "Model name:",          modelName);
        System.out.printf("  %-26s %s%n",  "Manufacturer:",         makerName);
        System.out.printf("  %-26s %s%n",  "Aircraft type:",        type);
        System.out.printf("  %-26s %s%n",  "Motorization:",         motorizationType);
        System.out.printf("  %-26s %.1f kg%n", "Empty weight:",     emptyWeight);
        System.out.printf("  %-26s %.1f kg%n", "MTOW:",             mtow);
        System.out.printf("  %-26s %.1f kg%n", "MZFW:",             mzfw);
        System.out.printf("  %-26s %.1f L%n",  "Max fuel capacity:", maxFuelCapacity);
        System.out.printf("  %-26s %.0f ft%n", "Service ceiling:",  serviceCeiling);
        System.out.printf("  %-26s %.1f km/h%n","Cruise speed:",    cruiseSpeed);
        System.out.printf("  %-26s %.1f km%n",  "Maximum range:",   maximumRange);
        System.out.printf("  %-26s %.2f m²%n",  "Wing area:",       wingArea);
        System.out.printf("  %-26s %.2f m%n",   "Wing span:",       wingSpan);
        System.out.printf("  %-26s %.2f%n",     "Aspect ratio:",    (wingSpan * wingSpan) / wingArea);
        System.out.printf("  %-26s %.4f%n",     "Drag coeff (Cd0):", dragCoefficient);
        System.out.printf("  %-26s %.4f%n",     "Lift coeff (Cl):",  liftCoefficient);
        System.out.println("  Certified engines:");
        certifiedEngines.forEach(e -> System.out.println("    • " + e));
        System.out.println();

        final String confirm = Console.readLine("  Confirm registration? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Registration cancelled.\n");
            return false;
        }

        try {
            controller.register(modelName, makerName, type, motorizationType,
                    emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                    wingArea, wingSpan, dragCoefficient, liftCoefficient, maximumRange, certifiedEngines);
            ok("Aircraft model \"" + modelName + "\" registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            error("Duplicate (model name, manufacturer) — this aircraft model already exists.");
        } catch (final IllegalArgumentException e) {
            error("Validation failed: " + e.getMessage());
        }
        return false;
    }

    private Set<EngineModel> selectEngines(final MotorizationType motorizationType) {
        final List<EngineModel> available = new ArrayList<>();
        controller.listEnginesByMotorizationType(motorizationType).forEach(available::add);

        if (available.isEmpty()) {
            System.out.println("  No engine models available for motorization type: " + motorizationType);
            return Set.of();
        }

        System.out.println("  Compatible engine models (type: " + motorizationType + "):");
        for (int i = 0; i < available.size(); i++) {
            System.out.printf("    %2d.  %s%n", i + 1, available.get(i));
        }
        System.out.println("  (Enter 0 to finish selecting)");

        final Set<EngineModel> selected = new HashSet<>();
        while (true) {
            final int choice = Console.readInteger("  Select engine (1-" + available.size() + ", 0 to finish)");
            if (choice == 0) break;
            if (choice >= 1 && choice <= available.size()) {
                final EngineModel pick = available.get(choice - 1);
                if (selected.add(pick)) {
                    System.out.println("  Added: " + pick);
                } else {
                    System.out.println("  (already selected)");
                }
            } else {
                System.out.println("  Invalid choice — enter a number between 1 and " + available.size() + ", or 0 to finish.");
            }
        }
        return selected;
    }

    private <T extends Enum<T>> T readEnum(final String label, final T[] values) {
        System.out.println(label + ":");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("    %2d.  %s%n", i + 1, values[i]);
        }
        final int choice = Console.readInteger("  Choice (1-" + values.length + ")");
        if (choice < 1 || choice > values.length) {
            throw new IllegalArgumentException("Invalid choice for " + label.trim());
        }
        return values[choice - 1];
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "REGISTER AIRCRAFT MODEL");
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
        return "Register Aircraft Model";
    }
}
