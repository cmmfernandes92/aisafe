package eapli.aisafeweb.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.application.GenerateSimulationReportController;
import eapli.aisafe.reporting.domain.InvolvedFlight;
import eapli.aisafe.reporting.domain.SafetyViolation;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.SimulationRunId;
import eapli.aisafe.reporting.domain.ValidationOutcome;
import eapli.aisafe.reporting.domain.ViolationKind;
import eapli.aisafe.reporting.infrastructure.SimulationReportWriter;
import eapli.aisafe.reporting.infrastructure.TextSimulationReportParser;

/**
 * Web adapter for the persisted simulation reports (US111):
 * <ul>
 *   <li>{@link #list()} reads the {@code SimulationRun} aggregates from the DB for
 *       the compact report list (no more hard-coded scenarios);</li>
 *   <li>{@link #detail(String)} maps one persisted {@code SimulationRun} onto the
 *       rich report-viewer payload so the report page is reachable again;</li>
 *   <li>{@link #generate(String)} feeds one run folder's {@code report.txt} to the
 *       core {@link GenerateSimulationReportController}, which parses it and
 *       persists one {@code SimulationRun}.</li>
 * </ul>
 * The controller must call {@code webSession.authenticateForControllerCall} before
 * {@link #generate} so the core use case's FCO/area authz passes.
 *
 * <p><b>Colour code</b> (shared by the list dot and the report banner): a run is
 * <em>green</em> when it PASSed with zero violations, <em>amber</em> when it PASSed
 * but recorded violations below the termination threshold, and <em>pink</em> when it
 * FAILed.</p>
 */
@Service
public class SimulationReportService {

    private static final String GREEN = "#3EE6A8";
    private static final String AMBER = "#FFB547";
    private static final String PINK = "#FF4D9D";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** IATA-ish route prefix → operator name, for the report's attribution view. */
    private static final Map<String, String> OPERATORS = Map.ofEntries(
            Map.entry("TP", "TAP Air Portugal"),
            Map.entry("FR", "Ryanair"),
            Map.entry("LH", "Lufthansa"),
            Map.entry("IB", "Iberia"),
            Map.entry("AF", "Air France"),
            Map.entry("BA", "British Airways"),
            Map.entry("EZY", "easyJet"));

    private final AreaSimRunsService runs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SimulationReportService(final AreaSimRunsService runs) {
        this.runs = runs;
    }

    /** Compact row for the report list. */
    public record SimRunView(String id, String scenario, String area, String timestamp,
            String verdict, String color, int flights, int violations, String endReason) {
    }

    /** Header fields the report page renders server-side (Thymeleaf). */
    public record ReportHeader(String simId, String date, String time, String scenarioName,
            String scenarioDesc, String sectorCode, String sectorName, String started,
            String terminated, String duration, int stepsDone, int totalSteps,
            String verdict, String endReason, String color) {
    }

    /** Everything the report viewer needs: the server-rendered header + the client JSON. */
    public record ReportDetail(ReportHeader run, String reportJson) {
    }

    /** All persisted runs, newest first. */
    public List<SimRunView> list() {
        final List<SimRunView> out = new ArrayList<>();
        for (final SimulationRun r : PersistenceContext.repositories().simulationRuns().findAll()) {
            out.add(new SimRunView(
                    r.identity().toString(),
                    r.scenario() == null || r.scenario().isBlank() ? "(unnamed)" : r.scenario(),
                    r.areaCode() == null ? "—" : r.areaCode().toString(),
                    r.runTimestamp() == null ? "" : TS.format(r.runTimestamp()),
                    r.validationOutcome().name(),
                    colorOf(r),
                    r.totalFlights(),
                    r.violations().size(),
                    r.endReason() == null ? "" : r.endReason()));
        }
        out.sort(Comparator.comparing(SimRunView::timestamp).reversed());
        return out;
    }

    /** Maps one persisted run (by id) onto the report-viewer payload, or empty if unknown. */
    public Optional<ReportDetail> detail(final String simId) {
        final SimulationRunId id;
        try {
            id = SimulationRunId.valueOf(simId);
        } catch (final RuntimeException malformed) {
            return Optional.empty();
        }
        return PersistenceContext.repositories().simulationRuns().ofIdentity(id).map(this::toDetail);
    }

    private ReportDetail toDetail(final SimulationRun r) {
        final boolean pass = r.validationOutcome() == ValidationOutcome.PASS;
        final int violations = r.violations().size();
        final LocalDateTime start = r.runTimestamp();
        final int maxStep = r.violations().stream().mapToInt(SafetyViolation::step).max().orElse(0);
        final long durationSecs = Math.round(maxStep * r.secondsPerStep());
        final LocalDateTime end = start == null ? null : start.plusSeconds(durationSecs);

        final String simRef = displayRef(r);
        final ReportHeader header = new ReportHeader(
                simRef,
                start == null ? "—" : DATE.format(start),
                start == null ? "—" : TIME.format(start),
                titleCase(r.scenario()),
                scenarioDesc(r.scenario()),
                r.areaCode() == null ? "—" : r.areaCode().value(),
                sectorName(r.areaCode()),
                start == null ? "—" : STAMP.format(start) + " UTC",
                end == null ? "—" : STAMP.format(end) + " UTC",
                formatDuration(durationSecs),
                maxStep,
                maxStep,
                r.validationOutcome().name(),
                endReasonLabel(r.endReason()),
                colorOf(r));

        // Derived threshold: the run's real max_violations is not persisted on the
        // aggregate, so we feed a verdict-consistent value (PASS ⇒ n, FAIL ⇒ n−1).
        // The report's pass/fail therefore matches the persisted ValidationOutcome.
        final int threshold = pass ? violations : Math.max(0, violations - 1);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("simId", simRef);
        payload.put("date", header.date());
        payload.put("time", header.time());
        payload.put("scenarioName", header.scenarioName());
        payload.put("scenarioDesc", header.scenarioDesc());
        payload.put("sectorCode", header.sectorCode());
        payload.put("sectorName", header.sectorName());
        payload.put("started", header.started());
        payload.put("terminated", header.terminated());
        payload.put("duration", header.duration());
        payload.put("stepsDone", maxStep);
        payload.put("totalSteps", maxStep);
        payload.put("threshold", threshold);
        payload.put("totalFlights", r.totalFlights());
        payload.put("verdict", header.verdict());
        payload.put("endReason", header.endReason());
        payload.put("operators", OPERATORS);
        payload.put("violations", r.violations().stream().map(v -> violationPayload(v, start)).toList());

        try {
            return new ReportDetail(header, objectMapper.writeValueAsString(payload));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise report payload", e);
        }
    }

    private Map<String, Object> violationPayload(final SafetyViolation v, final LocalDateTime runStart) {
        final InvolvedFlight first = v.involvedFlights().get(0);
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("t", v.timestamp() == null ? "" : TIME.format(v.timestamp()));
        m.put("step", v.step());
        m.put("x", round(first.x()));
        m.put("y", round(first.y()));
        m.put("alt", round(first.alt()));
        m.put("hDist", round(v.horizontalDistance()));
        m.put("vDist", round(v.verticalDistance()));
        m.put("type", typeLabel(v.kind()));
        m.put("sev", severity(v));
        m.put("flights", v.involvedFlights().stream().map(f -> shortRef(f.flightRef())).toList());
        m.put("desc", describe(v));
        return m;
    }

    /**
     * Parses the given run folder's {@code report.txt} and persists it as one
     * {@code SimulationRun} via the core US111 use case. The caller must have
     * authenticated the FCO into the authz session first.
     */
    public SimulationRun generate(final String runId) throws IOException {
        final Path runDir = runs.runDirOf(runId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown run folder: " + runId));
        final GenerateSimulationReportController controller = new GenerateSimulationReportController(
                new TextSimulationReportParser(),
                PersistenceContext.repositories().simulationRuns(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().customerCollaborators(),
                new SimulationReportWriter());
        return controller.generate(runDir, runDir.resolve("report-summary.txt"));
    }

    // ── derivations ──────────────────────────────────────────────────────────

    /** green = PASS &amp; clean, amber = PASS with violations, pink = FAIL. */
    private static String colorOf(final SimulationRun r) {
        if (r.validationOutcome() != ValidationOutcome.PASS) {
            return PINK;
        }
        return r.violations().isEmpty() ? GREEN : AMBER;
    }

    private static String displayRef(final SimulationRun r) {
        final String area = r.areaCode() == null ? "SIM" : r.areaCode().value();
        final String day = r.runTimestamp() == null ? "" : DATE.format(r.runTimestamp());
        final String tail = r.identity().toString().substring(0, 4).toUpperCase();
        return area + "-" + day + "-" + tail;
    }

    private static String sectorName(final AreaCode code) {
        if (code == null) {
            return "—";
        }
        return "LPPC".equals(code.value()) ? "Lisboa FIR" : code.value();
    }

    private static String scenarioDesc(final String scenario) {
        if (scenario == null) {
            return "Simulation run";
        }
        return switch (scenario.toLowerCase()) {
            case "clean" -> "Conflict-free traffic — no separation losses expected";
            case "violation" -> "Conflicts present but below the termination threshold";
            case "threshold" -> "Run terminates once violations breach the threshold";
            default -> "Simulation run";
        };
    }

    private static String endReasonLabel(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        return raw.replace('_', ' ').toUpperCase();
    }

    private static String typeLabel(final ViolationKind kind) {
        if (kind == null) {
            return "Safety violation";
        }
        return switch (kind) {
            case SEPARATION -> "Horizontal separation";
            case ALTITUDE_CEILING -> "Altitude ceiling";
            case ALTITUDE_FLOOR -> "Altitude floor";
            default -> "Safety violation";
        };
    }

    private static String severity(final SafetyViolation v) {
        if (v.kind() == ViolationKind.SEPARATION) {
            final double h = v.horizontalDistance();
            return h < 150 ? "high" : (h < 250 ? "medium" : "low");
        }
        final double d = v.verticalDistance();
        return d > 200 ? "high" : (d > 100 ? "medium" : "low");
    }

    private static String describe(final SafetyViolation v) {
        final long h = Math.round(v.horizontalDistance());
        final long ve = Math.round(v.verticalDistance());
        return switch (v.kind()) {
            case SEPARATION -> "Horizontal separation " + h + " m · vertical " + ve + " m.";
            case ALTITUDE_CEILING -> "Ceiling exceeded by " + ve + " m.";
            case ALTITUDE_FLOOR -> "Floor breached by " + ve + " m.";
            default -> "Safety violation at step " + v.step() + ".";
        };
    }

    /** Strips the {@code @yyyy-MM-dd} departure stamp the simulator appends to a route. */
    private static String shortRef(final String flightRef) {
        if (flightRef == null) {
            return "—";
        }
        final int at = flightRef.indexOf('@');
        return at > 0 ? flightRef.substring(0, at) : flightRef;
    }

    private static String titleCase(final String s) {
        if (s == null || s.isBlank()) {
            return "(unnamed)";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatDuration(final long totalSeconds) {
        final long h = totalSeconds / 3600;
        final long m = (totalSeconds % 3600) / 60;
        final long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static double round(final double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}