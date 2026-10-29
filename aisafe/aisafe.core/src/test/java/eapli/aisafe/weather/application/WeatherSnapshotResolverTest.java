package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;

public class WeatherSnapshotResolverTest {

    private static final DataSource SOURCE = new DataSource("Open-Meteo");
    private static final DataSource OTHER_SOURCE = new DataSource("IPMA");
    private static final TimePeriod PERIOD = new TimePeriod(
            LocalDateTime.of(2026, 6, 22, 5, 0),
            LocalDateTime.of(2026, 6, 22, 7, 30));
    private static final TimePeriod OTHER_PERIOD = new TimePeriod(
            LocalDateTime.of(2026, 6, 22, 8, 0),
            LocalDateTime.of(2026, 6, 22, 10, 0));

    private final FakeWeatherDataRepository repo = new FakeWeatherDataRepository();
    private final WeatherSnapshotResolver resolver = new WeatherSnapshotResolver(repo);

    @Test
    public void ensureResolveReturnsForecastWhoseBoxContainsSegmentMidpoint() {
        final WeatherData inside = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 0.0, 2000.0);
        repo.seed(inside);

        final Optional<WeatherData> resolved = resolver.resolve(
                segment(38.5, -9.5, 39.5, -8.5, 300.0), SOURCE, PERIOD);

        assertTrue(resolved.isPresent());
        assertEquals(inside.identity(), resolved.get().identity());
    }

    @Test
    public void ensureResolveReturnsEmptyWhenNoSnapshotBoxContainsMidpoint() {
        repo.seed(weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 39.0, -9.0, 0.0, 2000.0));

        final Optional<WeatherData> resolved = resolver.resolve(
                segment(39.5, -8.8, 39.8, -8.6, 300.0), SOURCE, PERIOD);

        assertFalse(resolved.isPresent());
    }

    @Test
    public void ensureResolveChoosesAltitudeBandContainingSegmentAltitude() {
        final WeatherData low = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 0.0, 1000.0);
        final WeatherData high = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 1001.0, 3000.0);
        repo.seed(low);
        repo.seed(high);

        final Optional<WeatherData> resolved = resolver.resolve(
                segment(38.5, -9.5, 39.5, -8.5, 500.0), SOURCE, PERIOD);

        assertTrue(resolved.isPresent());
        assertEquals(high.identity(), resolved.get().identity());
    }

    @Test
    public void ensureResolveSnapsInterBandGapToNearestBandCentre() {
        final WeatherData lower = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 0.0, 1000.0);
        final WeatherData upper = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 1002.0, 2000.0);
        repo.seed(lower);
        repo.seed(upper);

        final Optional<WeatherData> resolved = resolver.resolve(
                segment(38.5, -9.5, 39.5, -8.5,
                        1020.0 / WeatherSnapshotResolver.METERS_TO_FEET), SOURCE, PERIOD);

        assertTrue(resolved.isPresent());
        assertEquals(upper.identity(), resolved.get().identity());
    }

    @Test
    public void ensureResolveSnapsAboveImportedCeilingToHighestBand() {
        final WeatherData lower = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 0.0, 1000.0);
        final WeatherData highest = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 1002.0, 2000.0);
        repo.seed(lower);
        repo.seed(highest);

        final Optional<WeatherData> resolved = resolver.resolve(
                segment(38.5, -9.5, 39.5, -8.5, 3000.0), SOURCE, PERIOD);

        assertTrue(resolved.isPresent());
        assertEquals(highest.identity(), resolved.get().identity());
    }

    @Test
    public void ensureListSnapshotsGroupsByDataSourceAndPeriod() {
        repo.seed(weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 39.0, -9.0, 0.0, 1000.0));
        repo.seed(weather("LECM", SOURCE, PERIOD,
                39.0, -8.0, 40.0, -7.0, 0.0, 1000.0));
        repo.seed(weather("LPPC", OTHER_SOURCE, PERIOD,
                38.0, -10.0, 39.0, -9.0, 0.0, 1000.0));
        repo.seed(weather("LPPC", SOURCE, OTHER_PERIOD,
                38.0, -10.0, 39.0, -9.0, 0.0, 1000.0));

        final List<WeatherSnapshot> snapshots = resolver.listSnapshots();

        assertEquals(3, snapshots.size());
        final WeatherSnapshot openMeteo = snapshots.stream()
                .filter(s -> s.source().equals(SOURCE) && s.period().equals(PERIOD))
                .findFirst().orElseThrow();
        assertEquals(2, openMeteo.recordCount());
        assertEquals(2, openMeteo.areas().size());
        assertTrue(openMeteo.areas().contains(AreaCode.valueOf("LPPC")));
        assertTrue(openMeteo.areas().contains(AreaCode.valueOf("LECM")));
    }

    @Test
    public void ensureMultiAreaSnapshotResolvesEachSegmentToItsOwnAreaRecord() {
        final WeatherData lppc = weather("LPPC", SOURCE, PERIOD,
                38.0, -10.0, 40.0, -8.0, 0.0, 40000.0);
        final WeatherData lecm = weather("LECM", SOURCE, PERIOD,
                40.0, -8.0, 42.0, -6.0, 0.0, 40000.0);
        repo.seed(lppc);
        repo.seed(lecm);

        final WeatherData first = resolver.resolve(
                segment(38.5, -9.5, 39.5, -8.5, 3000.0), SOURCE, PERIOD).orElseThrow();
        final WeatherData second = resolver.resolve(
                segment(40.5, -7.5, 41.5, -6.5, 3000.0), SOURCE, PERIOD).orElseThrow();

        assertEquals(lppc.identity(), first.identity());
        assertEquals(lecm.identity(), second.identity());
    }

    private static ParsedSegment segment(final double startLat, final double startLon,
            final double endLat, final double endLon, final double altitudeMeters) {
        return new ParsedSegment(1, 0, startLat, startLon, endLat, endLon,
                altitudeMeters, 270.0, 8.0);
    }

    private static WeatherData weather(final String areaCode, final DataSource source, final TimePeriod period,
            final double swLat, final double swLon, final double neLat, final double neLon,
            final double lowerFeet, final double upperFeet) {
        return new WeatherData(
                AreaCode.valueOf(areaCode),
                period,
                new WeatherZone(
                        new Coordinates(swLat, swLon),
                        new Coordinates(neLat, neLon),
                        new AltitudeBand(lowerFeet, upperFeet)),
                15.0, 1013.0, 8000.0,
                new WeatherWindSpeed(10.0),
                new WeatherWindDirection(270.0),
                source);
    }

    private static final class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new LinkedHashMap<>();

        void seed(final WeatherData entity) {
            data.put(entity.identity(), entity);
        }

        @Override
        public <S extends WeatherData> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<WeatherData> ofIdentity(final WeatherDataId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final WeatherDataId id) {
            return data.containsKey(id);
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
            return data.values().stream()
                    .filter(w -> w.areaCode().equals(areaCode)
                            && w.timePeriod().startTime().toLocalDate().equals(day))
                    .toList();
        }

        @Override
        public Iterable<WeatherData> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final WeatherData entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final WeatherDataId id) {
            data.remove(id);
        }
    }
}
