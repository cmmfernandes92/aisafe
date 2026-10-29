package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightmanagement.domain.physics.ResolvedSegment;
import eapli.aisafe.flightmanagement.domain.physics.WindSource;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;

/** US085 wind resolution: attached forecast overrides DSL baseline, with fallbacks. */
public class WindResolverTest {

    private final FakeWeatherDataRepository weather = new FakeWeatherDataRepository();
    private final WindResolver resolver = new WindResolver(weather);

    private static ParsedFlightPlan twoSegmentPlan() {
        return new ParsedFlightPlan("FR401", "LPPR", "LPPT",
                LocalDateTime.parse("2026-06-12T13:00"), LocalDateTime.parse("2026-06-12T13:50"),
                3300, ParsedFlightPlan.FuelUnit.KG,
                List.of(
                        new ParsedSegment(1, 0, 41.0, -8.0, 40.0, -8.5, 9000, 350, 8.0),
                        new ParsedSegment(1, 1, 40.0, -8.5, 39.0, -9.0, 9000, 340, 6.5)));
    }

    @Test
    public void ensureNoAttachmentUsesDslBaselineWind() {
        final List<ResolvedSegment> resolved = resolver.resolve(twoSegmentPlan(), Map.of());

        assertEquals(WindSource.DSL_BASELINE, resolved.get(0).windSource());
        assertEquals(350.0, resolved.get(0).windDirectionDeg(), 1e-9);
        assertEquals(8.0, resolved.get(0).windSpeedMps(), 1e-9);
    }

    @Test
    public void ensureAttachedForecastOverridesAndConvertsKnotsToMetersPerSecond() {
        final WeatherData forecast = weather.saveWithWind(20.0, 90.0); // 20 kt, 90°
        final Map<SegmentKey, WeatherDataId> attach = Map.of(SegmentKey.of(1, 0), forecast.identity());

        final List<ResolvedSegment> resolved = resolver.resolve(twoSegmentPlan(), attach);

        assertEquals(WindSource.ATTACHED_FORECAST, resolved.get(0).windSource());
        assertEquals(90.0, resolved.get(0).windDirectionDeg(), 1e-9);
        assertEquals(20.0 * WindResolver.KNOTS_TO_MPS, resolved.get(0).windSpeedMps(), 1e-9);
        // second segment had no attachment → DSL baseline
        assertEquals(WindSource.DSL_BASELINE, resolved.get(1).windSource());
    }

    @Test
    public void ensureDanglingWeatherIdFallsBackToDslWithoutCrashing() {
        // a key attached to an id the repository does not contain (deleted after attach)
        final Map<SegmentKey, WeatherDataId> attach = new HashMap<>();
        attach.put(SegmentKey.of(1, 0), WeatherDataId.newId());

        final List<ResolvedSegment> resolved = resolver.resolve(twoSegmentPlan(), attach);

        assertEquals(WindSource.DSL_BASELINE, resolved.get(0).windSource());
        assertEquals(8.0, resolved.get(0).windSpeedMps(), 1e-9);
    }

    private static final class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

        WeatherData saveWithWind(final double knots, final double dirDeg) {
            final WeatherData wd = new WeatherData(AreaCode.valueOf("LPPO"),
                    new TimePeriod(LocalDateTime.parse("2026-06-12T12:00"),
                            LocalDateTime.parse("2026-06-12T15:00")),
                    new WeatherZone(new Coordinates(40.0, -10.0), new Coordinates(42.0, -8.0),
                            new AltitudeBand(0.0, 40000.0)),
                    new WeatherWindSpeed(knots), new WeatherWindDirection(dirDeg),
                    new DataSource("OpenMeteo"));
            data.put(wd.identity(), wd);
            return wd;
        }

        @Override public <S extends WeatherData> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<WeatherData> ofIdentity(final WeatherDataId id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean containsOfIdentity(final WeatherDataId id) { return data.containsKey(id); }
        @Override public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode a, final LocalDate d) {
            return data.values().stream().filter(w -> w.areaCode().equals(a)).toList();
        }
        @Override public Iterable<WeatherData> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final WeatherData e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final WeatherDataId id) { data.remove(id); }
    }
}