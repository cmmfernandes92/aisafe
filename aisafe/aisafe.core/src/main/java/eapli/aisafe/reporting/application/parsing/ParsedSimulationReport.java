package eapli.aisafe.reporting.application.parsing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Format-free DTO carrying a fully parsed C simulation report from the adapter to
 * the {@code SimulationReportAssembler}. The text layout
 * ({@code report_version=1}, {@code [flight_statuses]}, {@code [violations]}) never
 * leaks past the parser; the domain only ever sees this structure.
 */
public record ParsedSimulationReport(
        int reportVersion,
        LocalDateTime runTimestamp,
        String scenario,
        int totalSteps,
        int totalFlights,
        int maxViolations,
        double secondsPerStep,
        String endReason,
        String validation,
        List<ParsedFlightStatus> statuses,
        List<ParsedViolation> violations) {
}