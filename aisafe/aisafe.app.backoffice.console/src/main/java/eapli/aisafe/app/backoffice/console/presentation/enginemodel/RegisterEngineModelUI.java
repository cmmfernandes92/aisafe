package eapli.aisafe.app.backoffice.console.presentation.enginemodel;

import eapli.aisafe.enginemodelmanagement.application.RegisterEngineModelController;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterEngineModelUI extends AbstractUI {

    private static final String LINE = "─".repeat(52);

    private final RegisterEngineModelController controller = new RegisterEngineModelController(
            PersistenceContext.repositories().engineModels(),
            PersistenceContext.repositories().makers());

    @Override
    protected boolean doShow() {
        header();

        section("Identification");
        final String engineModelName = Console.readLine("  Engine model name");

        System.out.println();
        final java.util.List<eapli.aisafe.makermanagement.domain.Maker> makers = new java.util.ArrayList<>();
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

        section("Engine Type");
        final MotorizationType motorizationType = readEnum("  Motorization type", MotorizationType.values());
        System.out.println();
        final FuelType fuelType = readEnum("  Fuel type", FuelType.values());

        section("Performance");
        System.out.println("  Power : max (static, sea-level) thrust in N (e.g. 27000 N — turboprop, 120000 N — turbofan).");
        System.out.println("  Cruise thrust: thrust at cruise speed in N (2nd §3.3 thrust point; ~20% of static).");
        System.out.println("  TSFC  : Thrust Specific Fuel Consumption in kg/(N·h) — typical range 0.05 – 0.60.");
        System.out.println();
        final double power = Console.readDouble("  Power / static thrust (N, > 0)  ");
        final double cruiseThrust = Console.readDouble("  Cruise thrust (N, 0 < t ≤ power)");
        final double efficiency = Console.readDouble("  Efficiency (TSFC, > 0)          ");

        section("Summary");
        System.out.printf("  %-18s %s%n", "Name:",         engineModelName);
        System.out.printf("  %-18s %s%n", "Manufacturer:",  makerName);
        System.out.printf("  %-18s %s%n", "Type:",          motorizationType);
        System.out.printf("  %-18s %s%n", "Fuel:",          fuelType);
        System.out.printf("  %-18s %.1f N%n", "Power:",     power);
        System.out.printf("  %-18s %.1f N%n", "Cruise thrust:", cruiseThrust);
        System.out.printf("  %-18s %.4f%n", "TSFC:",        efficiency);
        System.out.println();

        final String confirm = Console.readLine("  Confirm registration? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Registration cancelled.\n");
            return false;
        }

        try {
            controller.register(engineModelName, makerName, motorizationType, fuelType, power, cruiseThrust, efficiency);
            ok("Engine model \"" + engineModelName + "\" registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            error("Duplicate (name, manufacturer) — this engine model already exists.");
        } catch (final IllegalArgumentException e) {
            error("Validation failed: " + e.getMessage());
        }
        return false;
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
        System.out.printf("  │  %-48s│%n", "REGISTER ENGINE MODEL");
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
        return "Register Engine Model";
    }
}
