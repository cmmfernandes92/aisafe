package eapli.aisafe.reporting.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.reporting.application.SimulationResultSource;
import eapli.aisafe.reporting.application.parsing.ParsedFlightStatus;
import eapli.aisafe.reporting.application.parsing.ParsedSimulationReport;
import eapli.aisafe.reporting.application.parsing.ParsedViolation;
import eapli.framework.validations.Preconditions;

/**
 * Adapter that understands the C simulator's {@code report.txt} text format and
 * the {@code flights.csv} surrogate-id sidecar, translating them into format-free
 * DTOs. The single place coupled to the {@code report_version=1} layout, so a
 * future layout change touches only this class (Protected Variation / OCP).
 */
public class TextSimulationReportParser implements SimulationResultSource {

    public static final String REPORT_FILE = "report.txt";
    public static final String SIDECAR_FILE = "flights.csv";

    private static final String STATUSES_HEADER = "[flight_statuses]";
    private static final String VIOLATIONS_HEADER = "[violations]";

    @Override
    public ParsedSimulationReport read(final Path runDir) throws IOException {
        Preconditions.nonNull(runDir);
        final Path reportPath = runDir.resolve(REPORT_FILE);
        if (!Files.isRegularFile(reportPath)) {
            throw new IOException("No " + REPORT_FILE + " in run folder " + runDir);
        }
        final String reportText = Files.readString(reportPath, StandardCharsets.UTF_8);

        final Path sidecarPath = runDir.resolve(SIDECAR_FILE);
        final Map<Integer, FlightId> idMap = Files.isRegularFile(sidecarPath)
                ? parseSidecar(Files.readString(sidecarPath, StandardCharsets.UTF_8))
                : Map.of();

        return parse(reportText, idMap);
    }

    /**
     * Parses already-loaded report text against a surrogate-id map. Pure (no I/O),
     * so it is directly unit-testable.
     */
    public ParsedSimulationReport parse(final String reportText, final Map<Integer, FlightId> idMap) {
        Preconditions.noneNull(reportText, idMap);

        final Map<String, String> header = new HashMap<>();
        final List<ParsedFlightStatus> statuses = new ArrayList<>();
        final List<ParsedViolation> violations = new ArrayList<>();

        Section section = Section.HEADER;
        for (final String rawLine : reportText.split("\\R")) {
            final String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals(STATUSES_HEADER)) {
                section = Section.STATUSES;
                continue;
            }
            if (line.equals(VIOLATIONS_HEADER)) {
                section = Section.VIOLATIONS;
                continue;
            }
            switch (section) {
                case HEADER:
                    final int eq = line.indexOf('=');
                    if (eq > 0) {
                        header.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                    break;
                case STATUSES:
                    if (line.startsWith("flight_id")) {
                        continue; // column header
                    }
                    statuses.add(parseStatus(line, idMap));
                    break;
                case VIOLATIONS:
                    if (line.startsWith("step")) {
                        continue; // column header
                    }
                    violations.add(parseViolation(line, idMap));
                    break;
                default:
                    break;
            }
        }

        return new ParsedSimulationReport(
                intValue(header, "report_version", 1),
                LocalDateTime.parse(required(header, "run_timestamp")),
                header.getOrDefault("scenario", ""),
                intValue(header, "total_steps", 0),
                intValue(header, "total_flights", statuses.size()),
                intValue(header, "max_violations", 0),
                doubleValue(header, "seconds_per_step", 60.0),
                header.getOrDefault("end_reason", ""),
                required(header, "validation"),
                statuses,
                violations);
    }

    private static ParsedFlightStatus parseStatus(final String line, final Map<Integer, FlightId> idMap) {
        final String[] f = line.split(",");
        if (f.length < 3) {
            throw new IllegalArgumentException("Malformed flight_statuses row: " + line);
        }
        final int flightId = Integer.parseInt(f[0].trim());
        return new ParsedFlightStatus(flightId, f[1].trim(), Integer.parseInt(f[2].trim()),
                idMap.get(flightId));
    }

    private static ParsedViolation parseViolation(final String line, final Map<Integer, FlightId> idMap) {
        final String[] f = line.split(",");
        if (f.length < 17) {
            throw new IllegalArgumentException("Malformed violations row (expected 17 columns): " + line);
        }
        final int step = Integer.parseInt(f[0].trim());
        final int flight1 = Integer.parseInt(f[1].trim());
        final int flight2 = Integer.parseInt(f[2].trim());
        final String kind = f.length >= 18 ? f[17].trim() : "separation";
        return new ParsedViolation(step, flight1, flight2,
                d(f[3]), d(f[4]),
                d(f[5]), d(f[6]), d(f[7]), d(f[8]), d(f[9]), d(f[10]),
                d(f[11]), d(f[12]), d(f[13]), d(f[14]), d(f[15]), d(f[16]),
                idMap.get(flight1), idMap.get(flight2), kind);
    }

    /** Parses {@code surrogate_id,route,departure_date} rows into the reverse map. */
    private static Map<Integer, FlightId> parseSidecar(final String csv) {
        final Map<Integer, FlightId> idMap = new LinkedHashMap<>();
        for (final String rawLine : csv.split("\\R")) {
            final String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("surrogate_id")) {
                continue;
            }
            final String[] f = line.split(",");
            if (f.length < 3) {
                continue;
            }
            final int surrogateId = Integer.parseInt(f[0].trim());
            final FlightId flightId = new FlightId(
                    RouteName.valueOf(f[1].trim()), LocalDate.parse(f[2].trim()));
            idMap.put(surrogateId, flightId);
        }
        return idMap;
    }

    private static double d(final String s) {
        return Double.parseDouble(s.trim());
    }

    private static String required(final Map<String, String> header, final String key) {
        final String value = header.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Report is missing required header: " + key);
        }
        return value;
    }

    private static int intValue(final Map<String, String> header, final String key, final int fallback) {
        final String value = header.get(key);
        return value == null ? fallback : Integer.parseInt(value.trim());
    }

    private static double doubleValue(final Map<String, String> header, final String key, final double fallback) {
        final String value = header.get(key);
        return value == null ? fallback : Double.parseDouble(value.trim());
    }

    private enum Section {
        HEADER, STATUSES, VIOLATIONS
    }
}