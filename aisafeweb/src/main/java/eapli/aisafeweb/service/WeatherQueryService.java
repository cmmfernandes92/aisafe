package eapli.aisafeweb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.weather.domain.WeatherData;

@Service
public class WeatherQueryService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter WINDOW = DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.US);

    /**
     * Immediate planning horizon an air control weather service acts on. A forecast
     * is "operationally relevant" when its validity period overlaps [now, now + horizon].
     * Kept as a single constant so widening it (48h, 72h) is a one-line change.
     */
    private static final int OPERATIONAL_HORIZON_HOURS = 24;

    public WeatherDashboardSnapshot dashboard() {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime horizon = now.plusHours(OPERATIONAL_HORIZON_HOURS);

        final List<AirControlArea> areas = areas();
        final List<WeatherData> weatherData = weatherData();
        final Map<String, List<WeatherData>> recordsByArea = weatherData.stream()
                .collect(Collectors.groupingBy(data -> data.areaCode().toString()));

        final List<WeatherAreaSummary> summaries = areas.stream()
                .map(area -> summarize(
                        area.code().toString(),
                        area.name().toString(),
                        recordsByArea.getOrDefault(area.code().toString(), List.of()),
                        now, horizon))
                .toList();
        final long covered = summaries.stream().filter(s -> "covered".equals(s.statusClass())).count();
        final long gaps = areas.size() - covered;
        final long providers = weatherData.stream()
                .map(data -> data.dataSource().value())
                .distinct()
                .count();

        final List<Kpi> kpis = List.of(
                new Kpi("Air Control Areas", areas.size(), "Monitored in AISafe"),
                new Kpi("Coverage Gaps", gaps,
                        "No forecast valid in the next " + OPERATIONAL_HORIZON_HOURS + "h"),
                new Kpi("Covered Now", covered,
                        "Forecast valid within the next " + OPERATIONAL_HORIZON_HOURS + "h"),
                new Kpi("Data Providers", providers, weatherData.size() + " records across all sources"));

        return new WeatherDashboardSnapshot(
                kpis,
                weatherAreaMapViews(areas, weatherCountsByArea(weatherData)),
                summaries,
                weatherRows(weatherData.stream().limit(8).toList()),
                gaps);
    }

    private WeatherAreaSummary summarize(
            final String code,
            final String name,
            final List<WeatherData> records,
            final LocalDateTime now,
            final LocalDateTime horizon) {
        if (records.isEmpty()) {
            return new WeatherAreaSummary(code, name, 0, false, "No data", "missing", "-");
        }
        final boolean covered = records.stream().anyMatch(record ->
                !record.timePeriod().startTime().isAfter(horizon)
                        && !record.timePeriod().endTime().isBefore(now));
        final LocalDateTime earliest = records.stream()
                .map(record -> record.timePeriod().startTime())
                .min(Comparator.naturalOrder()).orElseThrow();
        final LocalDateTime latest = records.stream()
                .map(record -> record.timePeriod().endTime())
                .max(Comparator.naturalOrder()).orElseThrow();
        final String window = WINDOW.format(earliest) + " -> " + WINDOW.format(latest);
        return covered
                ? new WeatherAreaSummary(code, name, records.size(), true, "Covered", "covered", window)
                : new WeatherAreaSummary(code, name, records.size(), true,
                        "Outside " + OPERATIONAL_HORIZON_HOURS + "h window", "outside", window);
    }

    public WeatherDataPageSnapshot weatherDataPage() {
        return new WeatherDataPageSnapshot(areas(), weatherRows(weatherData()));
    }

    public List<AirControlArea> areas() {
        return toList(PersistenceContext.repositories().airControlAreas().findAll()).stream()
                .sorted(Comparator.comparing(a -> a.code().toString()))
                .toList();
    }

    public List<WeatherDataRow> weatherRows() {
        return weatherRows(weatherData());
    }

    private List<WeatherData> weatherData() {
        return toList(PersistenceContext.repositories().weatherData().findAll()).stream()
                .sorted(Comparator.comparing((WeatherData data) -> data.timePeriod().startTime()).reversed()
                        .thenComparing(data -> data.areaCode().toString()))
                .toList();
    }

    private List<WeatherDataRow> weatherRows(final List<WeatherData> source) {
        return source.stream()
                .map(data -> new WeatherDataRow(
                        data.identity().toString(),
                        data.areaCode().toString(),
                        data.timePeriod().startTime().format(DATE_TIME),
                        data.timePeriod().endTime().format(DATE_TIME),
                        format(data.zone().southwest().latitude(), 4),
                        format(data.zone().southwest().longitude(), 4),
                        format(data.zone().northeast().latitude(), 4),
                        format(data.zone().northeast().longitude(), 4),
                        format(data.zone().altitudeBand().lowerFeet(), 0),
                        format(data.zone().altitudeBand().upperFeet(), 0),
                        optional(data.temperature(), 1),
                        optional(data.pressure(), 1),
                        optional(data.visibility(), 1),
                        format(data.windSpeed().value(), 1),
                        format(data.windDirection().value(), 0),
                        data.dataSource().value()))
                .toList();
    }

    private Map<String, Long> weatherCountsByArea(final List<WeatherData> weatherData) {
        final Map<String, Long> counts = new LinkedHashMap<>();
        for (final WeatherData data : weatherData) {
            counts.merge(data.areaCode().toString(), 1L, Long::sum);
        }
        return counts;
    }

    private List<WeatherAreaMapView> weatherAreaMapViews(
            final List<AirControlArea> areas,
            final Map<String, Long> weatherByArea) {
        return areas.stream()
                .map(area -> new WeatherAreaMapView(
                        area.code().toString(),
                        area.name().toString(),
                        area.boundary().southwest().latitude(),
                        area.boundary().southwest().longitude(),
                        area.boundary().northeast().latitude(),
                        area.boundary().northeast().longitude(),
                        (area.boundary().southwest().latitude() + area.boundary().northeast().latitude()) / 2,
                        (area.boundary().southwest().longitude() + area.boundary().northeast().longitude()) / 2,
                        weatherByArea.getOrDefault(area.code().toString(), 0L),
                        weatherByArea.getOrDefault(area.code().toString(), 0L) > 0))
                .toList();
    }

    private static String optional(final Double value, final int decimals) {
        return value == null ? "-" : format(value, decimals);
    }

    private static String format(final double value, final int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private static <T> List<T> toList(final Iterable<T> source) {
        final List<T> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    public record Kpi(String label, long value, String caption) {
    }

    public record WeatherAreaMapView(
            String code,
            String name,
            double swLat,
            double swLon,
            double neLat,
            double neLon,
            double centerLat,
            double centerLon,
            long weatherRecordCount,
            boolean hasWeatherData) {
    }

    public record WeatherAreaSummary(
            String code,
            String name,
            long weatherRecordCount,
            boolean hasWeatherData,
            String status,
            String statusClass,
            String coverageWindow) {
    }

    public record WeatherDataRow(
            String id,
            String areaCode,
            String startTime,
            String endTime,
            String zoneSwLat,
            String zoneSwLon,
            String zoneNeLat,
            String zoneNeLon,
            String altLowerFeet,
            String altUpperFeet,
            String temperature,
            String pressure,
            String visibility,
            String windSpeed,
            String windDirection,
            String dataSource) {
    }

    public record WeatherDashboardSnapshot(
            List<Kpi> kpis,
            List<WeatherAreaMapView> areaMap,
            List<WeatherAreaSummary> areas,
            List<WeatherDataRow> latestWeather,
            long coverageGapCount) {
    }

    public record WeatherDataPageSnapshot(
            List<AirControlArea> areas,
            List<WeatherDataRow> weatherData) {
    }
}
