package eapli.aisafe.weather.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.repositories.WeatherDataRepository;

/**
 * Resolves flight-plan segments to weather records from a selected snapshot.
 *
 * <p>Matching rules:</p>
 * <ul>
 *   <li>Snapshot identity is the derived {@code (dataSource, timePeriod)} pair.</li>
 *   <li>The segment point is its midpoint:
 *       {@code ((startLat + endLat) / 2, (startLon + endLon) / 2)}.</li>
 *   <li>A weather row covers the point when its zone box contains that midpoint,
 *       inclusive of all boundaries.</li>
 *   <li>Segment altitude is converted from metres to feet using {@code 3.28084}.</li>
 *   <li>If an altitude band contains the altitude, that row wins. If no band
 *       contains it exactly, the nearest band by centre is selected. This bridges
 *       1-ft inter-band gaps and snaps cruise altitudes above the imported ceiling
 *       to the highest available band within the matching box.</li>
 * </ul>
 *
 * <p>This service is read-only. US082 still writes through
 * {@code AttachWeatherToPlanController} with {@code SegmentKey -> WeatherDataId}
 * assignments.</p>
 */
public class WeatherSnapshotResolver {

    static final double METERS_TO_FEET = 3.28084;

    private final WeatherDataRepository weatherRepository;

    public WeatherSnapshotResolver(final WeatherDataRepository weatherRepository) {
        if (weatherRepository == null) {
            throw new IllegalArgumentException("Weather repository must not be null");
        }
        this.weatherRepository = weatherRepository;
    }

    public List<WeatherSnapshot> listSnapshots() {
        final Map<SnapshotKey, SnapshotAccumulator> grouped = new LinkedHashMap<>();
        for (final WeatherData data : orderedWeatherData()) {
            grouped.computeIfAbsent(new SnapshotKey(data.dataSource(), data.timePeriod()),
                    key -> new SnapshotAccumulator(key.source(), key.period()))
                    .add(data.areaCode());
        }
        return grouped.values().stream()
                .map(SnapshotAccumulator::snapshot)
                .sorted(Comparator.comparing((WeatherSnapshot s) -> s.period().startTime())
                        .thenComparing(s -> s.period().endTime())
                        .thenComparing(s -> s.source().value()))
                .toList();
    }

    public Optional<WeatherData> resolve(final ParsedSegment segment,
            final DataSource source, final TimePeriod period) {
        if (segment == null) {
            throw new IllegalArgumentException("Segment must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Data source must not be null");
        }
        if (period == null) {
            throw new IllegalArgumentException("Time period must not be null");
        }

        final double lat = (segment.startLatitude() + segment.endLatitude()) / 2.0;
        final double lon = (segment.startLongitude() + segment.endLongitude()) / 2.0;
        final double altitudeFeet = segment.altitudeMeters() * METERS_TO_FEET;

        final List<WeatherData> pointCandidates = orderedWeatherData().stream()
                .filter(data -> data.dataSource().equals(source))
                .filter(data -> data.timePeriod().equals(period))
                .filter(data -> contains(data, lat, lon))
                .toList();
        if (pointCandidates.isEmpty()) {
            return Optional.empty();
        }

        return pointCandidates.stream()
                .filter(data -> contains(data.zone().altitudeBand(), altitudeFeet))
                .findFirst()
                .or(() -> pointCandidates.stream()
                        .min(Comparator.comparingDouble(data ->
                                distanceFromBandCentre(data.zone().altitudeBand(), altitudeFeet))));
    }

    private List<WeatherData> orderedWeatherData() {
        final List<WeatherData> data = new ArrayList<>();
        weatherRepository.findAll().forEach(data::add);
        data.sort(Comparator
                .comparing((WeatherData wd) -> wd.dataSource().value())
                .thenComparing(wd -> wd.timePeriod().startTime())
                .thenComparing(wd -> wd.timePeriod().endTime())
                .thenComparing(wd -> wd.areaCode().toString())
                .thenComparingDouble(wd -> wd.zone().southwest().latitude())
                .thenComparingDouble(wd -> wd.zone().southwest().longitude())
                .thenComparingDouble(wd -> wd.zone().altitudeBand().lowerFeet())
                .thenComparing(wd -> wd.identity().toString()));
        return data;
    }

    private static boolean contains(final WeatherData data, final double lat, final double lon) {
        return data.zone().southwest().latitude() <= lat
                && lat <= data.zone().northeast().latitude()
                && data.zone().southwest().longitude() <= lon
                && lon <= data.zone().northeast().longitude();
    }

    private static boolean contains(final AltitudeBand band, final double altitudeFeet) {
        return band.lowerFeet() <= altitudeFeet && altitudeFeet <= band.upperFeet();
    }

    private static double distanceFromBandCentre(final AltitudeBand band, final double altitudeFeet) {
        return Math.abs(((band.lowerFeet() + band.upperFeet()) / 2.0) - altitudeFeet);
    }

    private record SnapshotKey(DataSource source, TimePeriod period) {
    }

    private static final class SnapshotAccumulator {
        private final DataSource source;
        private final TimePeriod period;
        private final Set<AreaCode> areas = new LinkedHashSet<>();
        private int count;

        private SnapshotAccumulator(final DataSource source, final TimePeriod period) {
            this.source = source;
            this.period = period;
        }

        private void add(final AreaCode area) {
            areas.add(area);
            count++;
        }

        private WeatherSnapshot snapshot() {
            return new WeatherSnapshot(source, period, count, areas);
        }
    }
}
