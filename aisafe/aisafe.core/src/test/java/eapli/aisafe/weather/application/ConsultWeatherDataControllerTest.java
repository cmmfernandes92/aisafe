package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class ConsultWeatherDataControllerTest {

    private static final LocalDate TARGET_DAY = LocalDate.of(2024, 1, 15);
    private static final LocalDateTime START = TARGET_DAY.atTime(6, 0);
    private static final LocalDateTime END = TARGET_DAY.atTime(12, 0);
    private static final String AREA_LPPC = "LPPC";
    private static final String AREA_OTHER = "LEMD";

    private final FakeWeatherDataRepository weatherRepo = new FakeWeatherDataRepository();
    private final FakeAirControlAreaRepository areaRepo = new FakeAirControlAreaRepository();

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void setUp() {
        areaRepo.seedArea(AREA_LPPC, "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
    }

    @After
    public void tearDown() {
        AuthzTestHelper.clearSession();
    }

    private ConsultWeatherDataController newController() {
        return new ConsultWeatherDataController(weatherRepo, areaRepo);
    }

    private WeatherData makeRecord(final String areaCode, final LocalDateTime start, final LocalDateTime end) {
        return new WeatherData(
                AreaCode.valueOf(areaCode),
                new TimePeriod(start, end),
                new WeatherZone(
                        new Coordinates(38.0, -10.0),
                        new Coordinates(40.0, -8.0),
                        new AltitudeBand(0.0, 10000.0)),
                15.0, 1013.0, 8000.0,
                new WeatherWindSpeed(10.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    // AC US043.1 — Weather Person can consult weather data
    @Test
    public void ensureWeatherPersonCanConsultWeatherData() {
        AuthzTestHelper.loginAsWeatherPerson();
        weatherRepo.seed(makeRecord(AREA_LPPC, START, END));

        final Iterable<WeatherData> result = newController().consultWeatherData(AREA_LPPC, TARGET_DAY);

        assertNotNull(result);
        assertTrue(result.iterator().hasNext());
    }

    // AC US043.1 — Pilot can consult weather data
    @Test
    public void ensurePilotCanConsultWeatherData() {
        AuthzTestHelper.loginAsPilot();
        weatherRepo.seed(makeRecord(AREA_LPPC, START, END));

        final Iterable<WeatherData> result = newController().consultWeatherData(AREA_LPPC, TARGET_DAY);

        assertNotNull(result);
    }

    // AC US043.1 — Flight Control Operator can consult weather data
    @Test
    public void ensureFlightControlOperatorCanConsultWeatherData() {
        AuthzTestHelper.loginAsFlightControlOperator();
        weatherRepo.seed(makeRecord(AREA_LPPC, START, END));

        final Iterable<WeatherData> result = newController().consultWeatherData(AREA_LPPC, TARGET_DAY);

        assertNotNull(result);
    }

    // AC US043.1 — Unauthorized actor (Backoffice Operator) is rejected
    @Test(expected = UnauthorizedException.class)
    public void ensureUnauthorizedUserIsRejected() {
        AuthzTestHelper.loginAsBackofficeOperator();
        newController().consultWeatherData(AREA_LPPC, TARGET_DAY);
    }

    // AC US043.2 / US043.3 — Query returns only records matching the selected area and selected day
    @Test
    public void ensureQueryReturnsOnlyRecordsMatchingAreaAndDay() {
        AuthzTestHelper.loginAsWeatherPerson();

        final WeatherData matching = makeRecord(AREA_LPPC, START, END);

        final LocalDate otherDay = LocalDate.of(2024, 1, 16);
        final WeatherData wrongDay = makeRecord(AREA_LPPC, otherDay.atTime(6, 0), otherDay.atTime(12, 0));

        areaRepo.seedArea(AREA_OTHER, "Madrid FIR", 38.0, -5.0, 41.0, -3.0);
        final WeatherData wrongArea = makeRecord(AREA_OTHER, START, END);

        weatherRepo.seed(matching);
        weatherRepo.seed(wrongDay);
        weatherRepo.seed(wrongArea);

        final Iterable<WeatherData> result = newController().consultWeatherData(AREA_LPPC, TARGET_DAY);
        final List<WeatherData> list = new ArrayList<>();
        result.forEach(list::add);

        assertEquals(1, list.size());
        assertEquals(matching.identity(), list.get(0).identity());
    }

    // AC US043.4 — Empty result is handled gracefully (no exception, empty collection)
    @Test
    public void ensureEmptyResultDoesNotThrow() {
        AuthzTestHelper.loginAsWeatherPerson();

        final Iterable<WeatherData> result = newController().consultWeatherData(AREA_LPPC, TARGET_DAY);

        assertNotNull(result);
        assertFalse(result.iterator().hasNext());
    }

    // AC US043.2 — Unknown air control area is rejected with IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void ensureUnknownAreaCodeIsRejected() {
        AuthzTestHelper.loginAsWeatherPerson();
        newController().consultWeatherData("ZZZZ", TARGET_DAY);
    }

    // AC US043.1 — listAirControlAreas is accessible to all authorised actors
    @Test
    public void ensureWeatherPersonCanListAirControlAreas() {
        AuthzTestHelper.loginAsWeatherPerson();
        final Iterable<AirControlArea> areas = newController().listAirControlAreas();
        assertNotNull(areas);
        assertTrue(StreamSupport.stream(areas.spliterator(), false).findAny().isPresent());
    }

    // ── Fake repositories ────────────────────────────────────────────────────

    private static class FakeWeatherDataRepository implements WeatherDataRepository {

        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

        void seed(final WeatherData entity) {
            data.put(entity.identity(), entity);
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
            final List<WeatherData> result = new ArrayList<>();
            for (final WeatherData wd : data.values()) {
                if (wd.areaCode().equals(areaCode)
                        && wd.timePeriod().startTime().toLocalDate().equals(day)) {
                    result.add(wd);
                }
            }
            return result;
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

    private static class FakeAirControlAreaRepository implements AirControlAreaRepository {

        private final Map<AreaCode, AirControlArea> data = new HashMap<>();

        void seedArea(final String code, final String name,
                final double swLat, final double swLon,
                final double neLat, final double neLon) {
            final AirControlArea area = new AirControlArea(
                    new AreaCode(code),
                    new AreaName(name),
                    new GeographicBoundary(
                            new Coordinates(swLat, swLon),
                            new Coordinates(neLat, neLon)));
            data.put(area.identity(), area);
        }

        @Override
        public <S extends AirControlArea> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirControlArea> ofIdentity(final AreaCode id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final AreaCode id) {
            return data.containsKey(id);
        }

        @Override
        public Optional<AirControlArea> findByCode(final AreaCode code) {
            return Optional.ofNullable(data.get(code));
        }

        @Override
        public Optional<AirControlArea> findByName(final AreaName name) {
            return data.values().stream()
                    .filter(a -> a.name().equals(name))
                    .findFirst();
        }

        @Override
        public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
            return java.util.Collections.emptyList();
        }

        @Override
        public Iterable<AirControlArea> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final AirControlArea entity) {
            data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final AreaCode id) {
            data.remove(id);
        }
    }
}
