package eapli.aisafe.app.pilot.console.presentation.flightplan;

import eapli.aisafe.flightmanagement.application.ImportFlightPlanController;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporter;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporterFactory;
import eapli.aisafe.flightplanmanagement.dsl.CompilationResult;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanDTO;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImportFlightPlanUI extends AbstractUI {

    private final ImportFlightPlanController controller = new ImportFlightPlanController(
            PersistenceContext.repositories().flights());

    @Override
    protected boolean doShow() {
        header();

        // 1. Pilot selects one of their OWN flights as the import target.
        //    The flight is chosen explicitly here, not derived from the file.
        final Flight target = selectOwnFlight();
        if (target == null) {
            return false;
        }

        // 2. Point to the file and validate it (US120 lexical/syntactic/semantic).
        final Path path = Path.of(Console.readLine(
                "  Path to flight plan file " + supportedFormats() + ":").trim());

        if (!Files.exists(path)) {
            error("File not found: " + path.toAbsolutePath());
            return false;
        }

        final FlightPlanImporter importer = FlightPlanImporterFactory.forPath(path).orElse(null);
        if (importer == null) {
            error("Unsupported file format. Supported extensions: " + supportedFormats());
            return false;
        }

        System.out.println();
        System.out.println("  Validating file (lexical, syntactic and semantic analysis)…");

        final String rawContent;
        try {
            rawContent = Files.readString(path);
        } catch (final IOException e) {
            error("Could not read file: " + e.getMessage());
            return false;
        }

        final CompilationResult result;
        try (InputStream in = Files.newInputStream(path)) {
            result = importer.importFrom(in, path.getFileName().toString());
        } catch (final IOException e) {
            error("Could not compile file: " + e.getMessage());
            return false;
        }

        if (!result.ok()) {
            System.out.println();
            System.out.printf("  Validation FAILED — %d error(s):%n%n", result.errors().size());
            for (final DslError err : result.errors()) {
                System.out.printf("    [%s][%s] line %d:%d  %s%n",
                        err.phase(), err.code(), err.line(), err.column(), err.message());
            }
            System.out.println();
            return false;
        }

        final var flights = result.file().orElseThrow().flightPlans();
        if (flights.size() != 1) {
            error("This file describes " + flights.size() + " flight plans; "
                    + "import one plan per file into the selected flight.");
            return false;
        }

        final FlightPlanDTO dto = flights.get(0);
        section("Validation OK");
        System.out.printf("    %-12s  type=%-8s  %s → %s  (%d leg(s))%n",
                dto.designator().value(), dto.type(),
                dto.route().origin().code(), dto.route().destination().code(),
                dto.legs().size());
        System.out.println();

        // 3. Confirm and import into the selected flight.
        final String confirm = Console.readLine(
                "  Import this plan into flight " + target.route() + " on "
                        + target.departure().toLocalDate() + "? (y/n)");
        if (!"y".equalsIgnoreCase(confirm.trim())) {
            System.out.println("\n  Import cancelled.\n");
            return false;
        }

        try {
            final Flight flight = controller.importFlightPlan(target.identity(), rawContent);
            System.out.printf("%n  [ OK ]  Plan imported -> %s%n%n", flight.currentPlan().status());
        } catch (final ConcurrencyException | IllegalArgumentException | IllegalStateException e) {
            error(e.getMessage());
        }
        return false;
    }

    private Flight selectOwnFlight() {
        final List<Flight> flights = new ArrayList<>();
        controller.flightsOfAuthenticatedPilot().forEach(flights::add);

        if (flights.isEmpty()) {
            error("You have no flights. Create one first (US080) before importing a plan.");
            return null;
        }

        section("Your flights");
        for (int i = 0; i < flights.size(); i++) {
            final Flight f = flights.get(i);
            System.out.printf("    [%d]  %-10s  %s  aircraft=%-8s  plan=%s%n",
                    i + 1, f.route(), f.departure(), f.aircraft(), f.currentPlan().status());
        }
        System.out.println();

        final int choice;
        try {
            choice = Integer.parseInt(Console.readLine("  Select a flight (number):").trim());
        } catch (final NumberFormatException e) {
            error("Not a valid selection.");
            return null;
        }
        if (choice < 1 || choice > flights.size()) {
            error("Selection out of range.");
            return null;
        }
        return flights.get(choice - 1);
    }

    private static String supportedFormats() {
        return FlightPlanImporterFactory.supportedExtensions().stream()
                .map(e -> "*." + e)
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");
    }

    private void header() {
        System.out.println();
        System.out.println("  ┌" + "─".repeat(50) + "┐");
        System.out.printf("  │  %-48s│%n", "IMPORT FLIGHT PLAN FROM FILE");
        System.out.println("  └" + "─".repeat(50) + "┘");
        System.out.println();
    }

    private void section(final String title) {
        System.out.println();
        final String label = "── " + title + " ";
        System.out.println("  " + label + "─".repeat(Math.max(0, 50 - label.length())));
    }

    private void error(final String msg) {
        System.out.println();
        System.out.println("  [FAIL]  " + msg);
        System.out.println();
    }

    @Override
    public String headline() {
        return "Import Flight Plan from File";
    }
}