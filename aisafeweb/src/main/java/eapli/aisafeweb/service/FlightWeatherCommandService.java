package eapli.aisafeweb.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import eapli.aisafe.flightmanagement.application.AttachWeatherToPlanController;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.application.WeatherSnapshot;
import eapli.aisafe.weather.application.WeatherSnapshotResolver;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.aisafeweb.dsl.DslFlightPlanCompiler;

/**
 * Web adapter for US082 (Add Weather Data). The pilot picks one of their flights
 * with DSL content, the plan's segments are recovered by recompiling the stored
 * DSL through the same {@link DslFlightPlanCompiler} US085 uses (so the
 * {@link SegmentKey} written here matches the one US085 reads at resolve time),
 * and the pilot attaches a registered forecast per segment. All business rules
 * (pilot role, flight ownership, forecast existence, TESTED&nbsp;&rarr;&nbsp;VALIDATED
 * demotion) stay inside {@link AttachWeatherToPlanController} and the domain.
 */
@Service
public class FlightWeatherCommandService {

    private static final DateTimeFormatter SNAPSHOT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DslFlightPlanCompiler compiler;

    public FlightWeatherCommandService(final DslFlightPlanCompiler compiler) {
        this.compiler = compiler;
    }

    /** One pickable forecast: its id (for the form value) and a human label. */
    public record ForecastOption(String id, String label) {
    }

    /** One pickable snapshot: derived key plus source/period/count label. */
    public record SnapshotOption(String key, String label) {
    }

    /** One plan segment as a form row, with its resolved snapshot forecast if covered. */
    public record SegmentRow(int index, String label, String currentForecastId, ForecastOption resolvedForecast) {
        public boolean hasResolvedForecast() {
            return resolvedForecast != null;
        }
    }

    /**
     * The form model for a flight's add-weather page: snapshot choices, segment
     * rows, and any recompile errors. {@code ok} is false when the flight is
     * missing, not the pilot's, or its DSL won't recompile.
     */
    public record PlanWeatherView(boolean ok, List<String> errors, String flightKey,
            List<SegmentRow> segments, List<SnapshotOption> snapshots, String selectedSnapshotKey) {

        public static PlanWeatherView failed(final String flightKey, final List<String> errors) {
            return new PlanWeatherView(false, errors, flightKey, List.of(), List.of(), "");
        }

        public boolean hasSelectedSnapshot() {
            return selectedSnapshotKey != null && !selectedSnapshotKey.isBlank();
        }

        public long resolvedCount() {
            return segments.stream().filter(SegmentRow::hasResolvedForecast).count();
        }
    }

    /** Builds the add-weather form model for one of the pilot's own flights. */
    public PlanWeatherView planWeather(final String pilotEmail, final String route, final LocalDate date) {
        return planWeather(pilotEmail, route, date, null);
    }

    /** Builds the add-weather form model and resolves segments for the selected snapshot, if any. */
    public PlanWeatherView planWeather(final String pilotEmail, final String route, final LocalDate date,
            final String snapshotKey) {
        final String flightKey = route + "|" + date;
        final Flight flight = ownedFlight(pilotEmail, route, date);
        if (flight == null) {
            return PlanWeatherView.failed(flightKey,
                    List.of("No such flight, or it does not belong to you."));
        }

        final List<ParsedSegment> segments = recompileSegments(flight);
        if (segments == null) {
            return PlanWeatherView.failed(flightKey,
                    List.of("This flight's current plan has no valid DSL yet — import one first."));
        }

        final WeatherSnapshotResolver resolver = newResolver();
        final List<SnapshotOption> snapshots = snapshotOptions(resolver.listSnapshots());
        final SelectedSnapshot selected;
        try {
            selected = parseSnapshotKey(snapshotKey).orElse(null);
        } catch (final RuntimeException ex) {
            return PlanWeatherView.failed(flightKey, List.of(ex.getMessage()));
        }
        final Map<SegmentKey, WeatherDataId> current = flight.currentPlan().weatherAssignmentsBySegment();
        final List<SegmentRow> rows = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            final ParsedSegment s = segments.get(i);
            final WeatherDataId attached = current.get(new SegmentKey(s.legNumber(), s.segmentIndex()));
            final ForecastOption resolved = selected == null ? null
                    : resolver.resolve(s, selected.source(), selected.period())
                            .map(w -> new ForecastOption(w.identity().toString(), forecastLabel(w)))
                            .orElse(null);
            rows.add(new SegmentRow(i, segmentLabel(s), attached == null ? "" : attached.toString(), resolved));
        }

        return new PlanWeatherView(true, List.of(), flightKey, rows, snapshots,
                selected == null ? "" : snapshotKey);
    }

    /**
     * Attaches the chosen forecasts to the flight's current plan (US082). The
     * caller must have authenticated the pilot for the core controller call. The
     * segment ordinals come from recompiling the stored DSL, so they line up with
     * the indices the form rendered.
     *
     * @param forecastByIndex segment index &rarr; chosen forecast id (blank ids ignored)
     * @return the number of segments weather was attached to
     */
    public int attach(final String route, final LocalDate date, final Map<Integer, String> forecastByIndex) {
        final FlightId flightId = new FlightId(RouteName.valueOf(route), date);
        final Flight flight = PersistenceContext.repositories().flights().ofIdentity(flightId).orElse(null);
        if (flight == null) {
            throw new IllegalArgumentException("No flight found for " + route + " on " + date + ".");
        }
        final List<ParsedSegment> segments = recompileSegments(flight);
        if (segments == null) {
            throw new IllegalStateException("This flight's current plan has no valid DSL yet.");
        }

        final WeatherDataRepository weatherRepository = PersistenceContext.repositories().weatherData();
        final WeatherSnapshotResolver resolver = new WeatherSnapshotResolver(weatherRepository);
        final Map<SegmentKey, WeatherDataId> assignments = new LinkedHashMap<>();
        for (final Map.Entry<Integer, String> e : forecastByIndex.entrySet()) {
            final int index = e.getKey();
            final String forecastId = e.getValue();
            if (forecastId == null || forecastId.isBlank()) {
                continue;
            }
            if (index < 0 || index >= segments.size()) {
                throw new IllegalArgumentException("Unknown segment index: " + index);
            }
            final ParsedSegment s = segments.get(index);
            final WeatherDataId id = WeatherDataId.valueOf(forecastId.trim());
            final WeatherData chosen = weatherRepository.ofIdentity(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown weather data: " + id));
            final WeatherData resolved = resolver.resolve(s, chosen.dataSource(), chosen.timePeriod())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selected forecast does not cover " + segmentLabel(s) + "."));
            if (!resolved.identity().equals(chosen.identity())) {
                throw new IllegalArgumentException("Selected forecast does not cover " + segmentLabel(s) + ".");
            }
            assignments.put(new SegmentKey(s.legNumber(), s.segmentIndex()), id);
        }
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("No segment has weather coverage in the selected snapshot.");
        }

        new AttachWeatherToPlanController().attachWeather(flightId, assignments);
        return assignments.size();
    }

    private Flight ownedFlight(final String pilotEmail, final String route, final LocalDate date) {
        if (pilotEmail == null || route == null || date == null) {
            return null;
        }
        final Flight flight = PersistenceContext.repositories().flights()
                .ofIdentity(new FlightId(RouteName.valueOf(route), date)).orElse(null);
        if (flight == null || !flight.pilot().toString().equalsIgnoreCase(pilotEmail)) {
            return null;
        }
        return flight;
    }

    /** Recompiles the flight's current plan DSL to its segments, or null if it has none / won't compile. */
    private List<ParsedSegment> recompileSegments(final Flight flight) {
        final String dsl = flight.currentPlan().rawDslContent();
        if (dsl == null || dsl.isBlank()) {
            return null;
        }
        final FlightPlanCompilationResult result = compiler.compile(dsl);
        if (!result.ok() || result.plan().isEmpty()) {
            return null;
        }
        return result.plan().get().segments();
    }

    private WeatherSnapshotResolver newResolver() {
        return new WeatherSnapshotResolver(PersistenceContext.repositories().weatherData());
    }

    private static List<SnapshotOption> snapshotOptions(final List<WeatherSnapshot> snapshots) {
        return snapshots.stream()
                .map(snapshot -> new SnapshotOption(snapshotKey(snapshot), snapshotLabel(snapshot)))
                .toList();
    }

    private static String segmentLabel(final ParsedSegment s) {
        return String.format(Locale.US, "Leg %d · Seg %d · (%.3f, %.3f -> %.3f, %.3f) · %.0f m",
                s.legNumber(), s.segmentIndex(),
                s.startLatitude(), s.startLongitude(), s.endLatitude(), s.endLongitude(),
                s.altitudeMeters());
    }

    private static String forecastLabel(final WeatherData w) {
        return String.format(Locale.US, "%s · %.0f-%.0f ft · wind %.0f deg / %.1f kt · %s",
                w.areaCode(),
                w.zone().altitudeBand().lowerFeet(), w.zone().altitudeBand().upperFeet(),
                w.windDirection().value(), w.windSpeed().value(), w.dataSource());
    }

    private static String snapshotLabel(final WeatherSnapshot snapshot) {
        return String.format(Locale.US, "%s · %s -> %s · %d records · areas %s",
                snapshot.source().value(),
                snapshot.period().startTime().format(SNAPSHOT_TIME),
                snapshot.period().endTime().format(SNAPSHOT_TIME),
                snapshot.recordCount(),
                snapshot.areas());
    }

    private static String snapshotKey(final WeatherSnapshot snapshot) {
        return URLEncoder.encode(snapshot.source().value(), StandardCharsets.UTF_8)
                + "|" + snapshot.period().startTime()
                + "|" + snapshot.period().endTime();
    }

    private static Optional<SelectedSnapshot> parseSnapshotKey(final String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        final String[] parts = key.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid weather snapshot selection.");
        }
        return Optional.of(new SelectedSnapshot(
                new DataSource(URLDecoder.decode(parts[0], StandardCharsets.UTF_8)),
                new TimePeriod(LocalDateTime.parse(parts[1]), LocalDateTime.parse(parts[2]))));
    }

    private record SelectedSnapshot(DataSource source, TimePeriod period) {
    }
}
