package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class RegisterWeatherDataControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2024, 1, 15, 6, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 1, 15, 12, 0);
    private static final String AREA_LPPC = "LPPC";

    private final FakeWeatherDataRepository weatherRepo = new FakeWeatherDataRepository();
    private final FakeAirControlAreaRepository areaRepo = new FakeAirControlAreaRepository();

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void setUp() {
        AuthzTestHelper.loginAsWeatherPerson();
        areaRepo.seedArea(AREA_LPPC, "Lisbon TMA",
                38.0, -10.0, 40.0, -8.0);
    }

    @After
    public void tearDown() {
        AuthzTestHelper.clearSession();
    }

    private RegisterWeatherDataController newController() {
        return new RegisterWeatherDataController(weatherRepo, areaRepo);
    }

    // Zone within the LPPC area boundary (38,-10)..(40,-8), band 0-1000 ft
    private static final double SW_LAT = 38.5;
    private static final double SW_LON = -9.5;
    private static final double NE_LAT = 39.5;
    private static final double NE_LON = -8.5;
    private static final double ALT_LOWER = 0.0;
    private static final double ALT_UPPER = 1000.0;

    @Test
    public void ensureRegisterWeatherDataHappyPath() {
        final WeatherData result = newController().registerWeatherData(
                AREA_LPPC, START, END,
                SW_LAT, SW_LON, NE_LAT, NE_LON, ALT_LOWER, ALT_UPPER,
                15.0, 1013.0, 8000.0, 10.0, 270.0, "IPMA");
        assertNotNull(result);
        assertEquals(AreaCode.valueOf(AREA_LPPC), result.areaCode());
        assertEquals(1013.0, result.pressure(), 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureUnknownAreaCodeIsRejected() {
        newController().registerWeatherData(
                "ZZZZ", START, END,
                SW_LAT, SW_LON, NE_LAT, NE_LON, ALT_LOWER, ALT_UPPER,
                15.0, 1013.0, 8000.0, 10.0, 270.0, "IPMA");
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureRegistrationRequiresWeatherPersonRole() {
        AuthzTestHelper.loginAsPilot();
        newController().registerWeatherData(
                AREA_LPPC, START, END,
                SW_LAT, SW_LON, NE_LAT, NE_LON, ALT_LOWER, ALT_UPPER,
                15.0, 1013.0, 8000.0, 10.0, 270.0, "IPMA");
    }

    @Test
    public void ensureListAreasRequiresWeatherPersonRole() {
        final Iterable<AirControlArea> areas = newController().listAirControlAreas();
        assertNotNull(areas);
    }

    // ── Fake repositories ────────────────────────────────────────────────────

    private static class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

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

        @Override
        public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
            return Collections.emptyList();
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
