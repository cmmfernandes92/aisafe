package eapli.aisafeweb.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads finished SCOMP area-simulation runs off disk so the web 3D visualiser
 * can replay a <em>real</em> run instead of a hard-coded preset.
 *
 * <p>Each run is one sub-folder of {@code aisafeweb.scomp.runsRoot} (e.g.
 * {@code LPPC-20260613001819429}) containing the {@code scenario.json} the C
 * binary consumed plus its {@code report.txt}, {@code violations.log} and
 * {@code flights.csv}. The folder name is the run id; the scenario name
 * ({@code sim_params.name}, e.g. {@code LPPC-202606130200}) is shown in the
 * visualiser so several runs of the same scenario stay distinguishable.</p>
 *
 * <p>The C remains authoritative for the verdict — this service never recomputes
 * physics; it passes {@code scenario.json} straight through (the browser kernel
 * replays it) and reads the verdict/violations from {@code report.txt}.</p>
 */
@Service
public class AreaSimRunsService {

    private final Path runsRoot;
    private final ObjectMapper json = new ObjectMapper();

    public AreaSimRunsService(
            @Value("${aisafeweb.scomp.runsRoot:scomp/sprint-3/flight_sim/runs}") final String runsRoot) {
        this.runsRoot = resolveConfiguredPath(runsRoot);
    }

    // ── view models (client JSON contract) ───────────────────────────────────

    /** One row in the run picker. */
    public record RunSummary(String id, String scenario, String area, String runTimestamp,
            String verdict, String endReason, int flights, int violations) {
    }

    /** A single violation event from report.txt's [violations] section. */
    public record ViolationRow(int step, int flight1, int flight2, double hDist, double vDist, String kind) {
    }

    /** A surrogate-id → route mapping from flights.csv. */
    public record FlightIdentity(int surrogateId, String route, String departureDate) {
    }

    /** The full bundle the visualiser loads for one run. */
    public record RunBundle(String id, String scenario, String area, String runTimestamp,
            String verdict, String endReason, JsonNode params, JsonNode flights,
            List<FlightIdentity> idMap, List<ViolationRow> violations) {
    }

    // ── queries ──────────────────────────────────────────────────────────────

    /** All runs, newest folder first. Folders that fail to parse are skipped. */
    public List<RunSummary> list() {
        if (!Files.isDirectory(runsRoot)) {
            return List.of();
        }
        try (var entries = Files.list(runsRoot)) {
            return entries
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve("scenario.json")))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .map(this::summarize)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not list SCOMP runs in " + runsRoot, e);
        }
    }

    /** The full bundle for one run id (folder name), or empty if it doesn't exist. */
    public Optional<RunBundle> bundle(final String id) {
        final Path dir = resolveRunDir(id);
        if (dir == null) {
            return Optional.empty();
        }
        try {
            final JsonNode scenario = json.readTree(dir.resolve("scenario.json").toFile());
            final JsonNode params = scenario.path("sim_params");
            final Map<String, String> report = parseReport(dir.resolve("report.txt"));
            return Optional.of(new RunBundle(
                    id,
                    params.path("name").asText(id),
                    areaOf(id),
                    report.getOrDefault("run_timestamp", ""),
                    report.getOrDefault("validation", "—"),
                    report.getOrDefault("end_reason", "—"),
                    params,
                    scenario.path("flights"),
                    parseIdMap(dir.resolve("flights.csv")),
                    parseViolations(dir.resolve("report.txt"))));
        } catch (final IOException e) {
            throw new UncheckedIOException("Could not read SCOMP run " + id, e);
        }
    }

    // ── parsing ──────────────────────────────────────────────────────────────

    private Optional<RunSummary> summarize(final Path dir) {
        try {
            final String id = dir.getFileName().toString();
            final JsonNode params = json.readTree(dir.resolve("scenario.json").toFile()).path("sim_params");
            final Map<String, String> report = parseReport(dir.resolve("report.txt"));
            return Optional.of(new RunSummary(
                    id,
                    params.path("name").asText(id),
                    areaOf(id),
                    report.getOrDefault("run_timestamp", ""),
                    report.getOrDefault("validation", "—"),
                    report.getOrDefault("end_reason", "—"),
                    intOf(report, "total_flights"),
                    intOf(report, "reported_violation_count")));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    /** Reads the leading {@code key=value} block of report.txt (stops at the first [section]). */
    private static Map<String, String> parseReport(final Path reportTxt) throws IOException {
        final Map<String, String> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(reportTxt)) {
            return values;
        }
        for (final String line : Files.readAllLines(reportTxt, StandardCharsets.UTF_8)) {
            final String trimmed = line.strip();
            if (trimmed.startsWith("[")) {
                break;
            }
            final int eq = trimmed.indexOf('=');
            if (eq > 0) {
                values.put(trimmed.substring(0, eq).strip(), trimmed.substring(eq + 1).strip());
            }
        }
        return values;
    }

    /** Reads the {@code [violations]} CSV section of report.txt (header skipped). */
    private static List<ViolationRow> parseViolations(final Path reportTxt) throws IOException {
        final List<ViolationRow> rows = new ArrayList<>();
        if (!Files.isRegularFile(reportTxt)) {
            return rows;
        }
        boolean inSection = false;
        boolean headerSeen = false;
        for (final String line : Files.readAllLines(reportTxt, StandardCharsets.UTF_8)) {
            final String trimmed = line.strip();
            if (trimmed.equals("[violations]")) {
                inSection = true;
                continue;
            }
            if (!inSection) {
                continue;
            }
            if (trimmed.startsWith("[")) {
                break;
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!headerSeen) {
                headerSeen = true; // first row is the column header
                continue;
            }
            final String[] cols = trimmed.split(",");
            if (cols.length >= 5) {
                rows.add(new ViolationRow(
                        parseInt(cols[0]), parseInt(cols[1]), parseInt(cols[2]),
                        parseDouble(cols[3]), parseDouble(cols[4]),
                        cols.length >= 18 ? cols[17].strip() : "separation"));
            }
        }
        return rows;
    }

    private static List<FlightIdentity> parseIdMap(final Path flightsCsv) throws IOException {
        final List<FlightIdentity> ids = new ArrayList<>();
        if (!Files.isRegularFile(flightsCsv)) {
            return ids;
        }
        final List<String> lines = Files.readAllLines(flightsCsv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) { // skip header
            final String[] cols = lines.get(i).strip().split(",");
            if (cols.length >= 3 && !cols[0].isBlank()) {
                ids.add(new FlightIdentity(parseInt(cols[0]), cols[1].strip(), cols[2].strip()));
            }
        }
        return ids;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String areaOf(final String runId) {
        final int dash = runId.indexOf('-');
        return dash > 0 ? runId.substring(0, dash) : runId;
    }

    private static int intOf(final Map<String, String> report, final String key) {
        return parseInt(report.getOrDefault(key, "0"));
    }

    private static int parseInt(final String raw) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(final String raw) {
        try {
            return Double.parseDouble(raw.strip());
        } catch (final NumberFormatException e) {
            return 0.0;
        }
    }

    /** Resolves a run id (folder name) to its absolute directory, if it exists. */
    public Optional<Path> runDirOf(final String id) {
        return Optional.ofNullable(resolveRunDir(id));
    }

    /** Resolves a run id to its folder, rejecting anything that escapes runsRoot. */
    private Path resolveRunDir(final String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\") || id.contains("..")) {
            return null;
        }
        final Path dir = runsRoot.resolve(id).normalize();
        if (!dir.startsWith(runsRoot) || !Files.isDirectory(dir)
                || !Files.isRegularFile(dir.resolve("scenario.json"))) {
            return null;
        }
        return dir;
    }

    private static Path resolveConfiguredPath(final String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("runsRoot configuration must not be blank");
        }
        final Path path = Path.of(configured.strip());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        final Path cwd = Path.of("").toAbsolutePath().normalize();
        final Path direct = cwd.resolve(path).normalize();
        if (Files.exists(direct)) {
            return direct;
        }
        final Path parent = cwd.getParent() == null ? direct : cwd.getParent().resolve(path).normalize();
        if (Files.exists(parent)) {
            return parent;
        }
        return direct;
    }
}