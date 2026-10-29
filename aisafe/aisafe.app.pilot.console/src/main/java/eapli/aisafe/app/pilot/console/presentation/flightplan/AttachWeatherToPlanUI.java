package eapli.aisafe.app.pilot.console.presentation.flightplan;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.flightmanagement.application.AttachWeatherToPlanController;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.WeatherSnapshot;
import eapli.aisafe.weather.application.WeatherSnapshotResolver;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.framework.io.util.Console;

/**
 * US082 — the pilot attaches a registered weather forecast to one or more
 * segments of their flight plan, enriching it before the US085 test. The pilot
 * <em>selects</em> from forecasts already in the system (PO 25/05/2026); the
 * plan's segments are recovered by recompiling the stored DSL via the same
 * {@link ConsoleFlightPlanCompiler} US085 uses, so the {@link SegmentKey} written
 * here matches the one US085 reads at resolve time (no ordinal drift).
 */
public class AttachWeatherToPlanUI {

    private final AttachWeatherToPlanController controller = new AttachWeatherToPlanController();
    private final ConsoleFlightPlanCompiler compiler = new ConsoleFlightPlanCompiler();
    private final WeatherSnapshotResolver weatherResolver =
            new WeatherSnapshotResolver(PersistenceContext.repositories().weatherData());

    public void show() {
        System.out.println("\n=== Attach Weather To Flight Plan (US082) ===");

        final String route = Console.readLine("  Route name").trim();
        final String dateInput = Console.readLine("  Departure date (YYYY-MM-DD)").trim();

        final FlightId flightId;
        try {
            flightId = new FlightId(RouteName.valueOf(route), LocalDate.parse(dateInput));
        } catch (final RuntimeException e) {
            System.out.println("  [FAIL] Invalid route or date: " + e.getMessage());
            return;
        }

        final Flight flight = PersistenceContext.repositories().flights().ofIdentity(flightId).orElse(null);
        if (flight == null) {
            System.out.println("  [FAIL] No flight found for " + route + " on " + dateInput + ".");
            return;
        }

        final ParsedFlightPlan plan = compilePlan(flight);
        if (plan == null) {
            return;
        }

        final List<ParsedSegment> segments = plan.segments();
        final List<WeatherSnapshot> snapshots = weatherResolver.listSnapshots();
        if (snapshots.isEmpty()) {
            System.out.println("  [INFO] No weather forecasts are registered yet — register some first (US043/US083).");
            return;
        }

        printSegments(segments);
        printSnapshots(snapshots);

        final Integer snapshotIdx = readIndex("    Snapshot index", snapshots.size());
        if (snapshotIdx == null) {
            System.out.println("  [INFO] No snapshot selected.");
            return;
        }

        final WeatherSnapshot snapshot = snapshots.get(snapshotIdx);
        final Map<SegmentKey, WeatherDataId> assignments = resolveAssignments(segments, snapshot);
        if (assignments.isEmpty()) {
            System.out.println("  [INFO] No segments had weather coverage in that snapshot. DSL baseline wind is kept.");
            return;
        }

        try {
            controller.attachWeather(flightId, assignments);
            System.out.println("\n  [ OK ] Attached weather to " + assignments.size() + " segment(s).");
            System.out.println("         (a TESTED plan is demoted to VALIDATED — re-test after changing weather)");
        } catch (final RuntimeException e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private ParsedFlightPlan compilePlan(final Flight flight) {
        final String dsl = flight.currentPlan().rawDslContent();
        if (dsl == null || dsl.isBlank()) {
            System.out.println("  [FAIL] This flight's current plan has no DSL content yet (import one first, US121).");
            return null;
        }
        final FlightPlanCompilationResult result = compiler.compile(dsl);
        if (!result.ok() || result.plan().isEmpty()) {
            System.out.println("  [FAIL] Could not recompile the plan's DSL:");
            result.issues().forEach(i -> System.out.println("         - " + i.describe()));
            return null;
        }
        return result.plan().get();
    }

    private void printSegments(final List<ParsedSegment> segments) {
        System.out.println("\n  Plan segments:");
        for (int i = 0; i < segments.size(); i++) {
            final ParsedSegment s = segments.get(i);
            System.out.printf("    [%d]  leg %d seg %d  (%.4f,%.4f -> %.4f,%.4f)  alt %.0f m  baseline %.0f deg / %.1f m/s%n",
                    i, s.legNumber(), s.segmentIndex(), s.startLatitude(), s.startLongitude(),
                    s.endLatitude(), s.endLongitude(), s.altitudeMeters(),
                    s.baselineWindDirectionDeg(), s.baselineWindSpeedMps());
        }
    }

    private void printSnapshots(final List<WeatherSnapshot> snapshots) {
        System.out.println("\n  Weather snapshots:");
        for (int i = 0; i < snapshots.size(); i++) {
            final WeatherSnapshot snapshot = snapshots.get(i);
            System.out.printf("    [%d]  %s  %s  %d record(s)  areas %s%n",
                    i, snapshot.source(), snapshot.period(), snapshot.recordCount(), snapshot.areas());
        }
    }

    private Map<SegmentKey, WeatherDataId> resolveAssignments(final List<ParsedSegment> segments,
            final WeatherSnapshot snapshot) {
        final Map<SegmentKey, WeatherDataId> assignments = new LinkedHashMap<>();
        System.out.println("\n  Snapshot resolution:");
        for (int i = 0; i < segments.size(); i++) {
            final ParsedSegment seg = segments.get(i);
            final Optional<WeatherData> resolved =
                    weatherResolver.resolve(seg, snapshot.source(), snapshot.period());
            if (resolved.isEmpty()) {
                System.out.printf("    [%d]  leg %d seg %d  no coverage — DSL baseline kept%n",
                        i, seg.legNumber(), seg.segmentIndex());
                continue;
            }

            final WeatherData w = resolved.get();
            assignments.put(SegmentKey.of(seg.legNumber(), seg.segmentIndex()), w.identity());
            System.out.printf("    [%d]  leg %d seg %d  %-6s  band %.0f-%.0f ft  wind %.0f deg / %.1f kt  src %s%n",
                    i, seg.legNumber(), seg.segmentIndex(), w.areaCode(),
                    w.zone().altitudeBand().lowerFeet(), w.zone().altitudeBand().upperFeet(),
                    w.windDirection().value(), w.windSpeed().value(), w.dataSource());
        }
        return assignments;
    }

    /** Reads a 0-based index in [0, size), or {@code null} on blank/invalid input. */
    private Integer readIndex(final String prompt, final int size) {
        final String raw = Console.readLine(prompt + " (0-" + (size - 1) + ", blank to finish)").trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            final int idx = Integer.parseInt(raw);
            if (idx < 0 || idx >= size) {
                System.out.println("    (out of range)");
                return null;
            }
            return idx;
        } catch (final NumberFormatException e) {
            System.out.println("    (not a number)");
            return null;
        }
    }
}
