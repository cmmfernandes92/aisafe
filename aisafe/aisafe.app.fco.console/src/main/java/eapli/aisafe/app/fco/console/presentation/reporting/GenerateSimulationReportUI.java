package eapli.aisafe.app.fco.console.presentation.reporting;

import java.io.IOException;
import java.nio.file.Path;

import eapli.aisafe.reporting.application.GenerateSimulationReportController;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.ViolationKind;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * US111 console walkthrough (reuses {@code AbstractUI}'s Template Method): the FCO
 * points at a run folder, the controller ingests + persists it, and the operator
 * summary file is written.
 */
public class GenerateSimulationReportUI extends AbstractUI {

    private final GenerateSimulationReportController controller;

    public GenerateSimulationReportUI(final GenerateSimulationReportController controller) {
        this.controller = controller;
    }

    @Override
    public String headline() {
        return "Generate Simulation Report";
    }

    @Override
    protected boolean doShow() {
        final String dir = Console.readLine(
                "Simulation run folder (containing report.txt [+ flights.csv]): ");
        if (dir == null || dir.isBlank()) {
            System.out.println("No run folder given.");
            return false;
        }
        final String out = Console.readLine("Output summary file [report-summary.txt]: ");
        final Path runDir = Path.of(dir.trim());
        final Path output = Path.of(out == null || out.isBlank() ? "report-summary.txt" : out.trim());

        try {
            final SimulationRun run = controller.generate(runDir, output);
            System.out.println("Report generated.");
            System.out.println("  Verdict:    " + run.validationOutcome());
            System.out.println("  Flights:    " + run.totalFlights());
            System.out.println("  Violations: " + run.violations().size());
            if (!run.violations().isEmpty()) {
                final long separation = run.violations().stream()
                        .filter(v -> v.kind() == ViolationKind.SEPARATION).count();
                final long ceiling = run.violations().stream()
                        .filter(v -> v.kind() == ViolationKind.ALTITUDE_CEILING).count();
                final long floor = run.violations().stream()
                        .filter(v -> v.kind() == ViolationKind.ALTITUDE_FLOOR).count();
                System.out.println("    (separation: " + separation
                        + ", altitude ceiling: " + ceiling
                        + ", altitude floor: " + floor + ")");
            }
            System.out.println("  Area:       " + run.areaCode());
            System.out.println("  Summary:    " + output.toAbsolutePath());
        } catch (final IOException e) {
            System.out.println("Could not read the run folder: " + e.getMessage());
        } catch (final RuntimeException e) {
            System.out.println("Could not generate the report: " + e.getMessage());
        }
        return false;
    }
}