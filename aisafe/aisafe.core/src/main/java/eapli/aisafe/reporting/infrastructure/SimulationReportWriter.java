package eapli.aisafe.reporting.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import eapli.aisafe.reporting.domain.FlightExecutionStatus;
import eapli.aisafe.reporting.domain.InvolvedFlight;
import eapli.aisafe.reporting.domain.SafetyViolation;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.framework.validations.Preconditions;

/**
 * Renders the operator-facing summary file from a {@link SimulationRun} (US111.1).
 * A Pure Fabrication: a plain-text report is sufficient (PO 03/06/2026) and there
 * is a single C engine, so deliberately no Strategy / Template Method — that would
 * be premature abstraction here (US112 owns the report-format Strategy).
 */
public class SimulationReportWriter {

    public void write(final SimulationRun run, final Path outputFile) throws IOException {
        Preconditions.noneNull(run, outputFile);

        final StringBuilder sb = new StringBuilder();
        sb.append("AISafe Simulation Report\n");
        sb.append("========================\n");
        sb.append("Area:        ").append(run.areaCode()).append('\n');
        sb.append("Scenario:    ").append(run.scenario()).append('\n');
        sb.append("Run time:    ").append(run.runTimestamp()).append('\n');
        sb.append("End reason:  ").append(run.endReason()).append('\n');
        sb.append("Verdict:     ").append(run.validationOutcome()).append('\n');
        sb.append("Flights:     ").append(run.totalFlights()).append('\n');
        sb.append("Violations:  ").append(run.violations().size()).append('\n');

        sb.append("\nFlight statuses\n---------------\n");
        for (final FlightExecutionStatus status : run.flightStatuses()) {
            sb.append("  ").append(status.flightRef())
                    .append(" : ").append(status.outcome())
                    .append(" (exit ").append(status.exitCode()).append(")\n");
        }

        sb.append("\nSafety violations\n-----------------\n");
        if (run.violations().isEmpty()) {
            sb.append("  none\n");
        } else {
            for (final SafetyViolation v : run.violations()) {
                final InvolvedFlight first = v.involvedFlights().get(0);
                if (v.kind().isAltitude()) {
                    sb.append("  @").append(v.timestamp())
                            .append(" (step ").append(v.step()).append(") [")
                            .append(v.kind().name()).append("]: ")
                            .append(first.flightRef())
                            .append(" — altitude bust, ").append(v.verticalDistance())
                            .append("m past bound\n");
                } else {
                    final InvolvedFlight second = v.involvedFlights().get(1);
                    sb.append("  @").append(v.timestamp())
                            .append(" (step ").append(v.step()).append("): ")
                            .append(first.flightRef()).append(" vs ").append(second.flightRef())
                            .append(" — h=").append(v.horizontalDistance())
                            .append("m v=").append(v.verticalDistance()).append("m\n");
                }
            }
        }

        final Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
    }
}