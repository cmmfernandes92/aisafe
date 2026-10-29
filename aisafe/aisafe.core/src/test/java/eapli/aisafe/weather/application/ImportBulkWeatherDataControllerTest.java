package eapli.aisafe.weather.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.SourceType;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReaderFactory;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class ImportBulkWeatherDataControllerTest {

    private static final String AREA = "LPPC";
    private static final String SOURCE = "Happy Weather";

    private static final String HEADER =
            "SW Lat,SW Lon,NE Lat,NE Lon,Alt_inf (ft),Alt_Sup (ft),Direction (degree),Value (knot),Day,Star,End";
    private static final String VALID_ROW_1 =
            "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30";
    private static final String VALID_ROW_2 =
            "38.0,-10.0,39.0,-9.5,0,1000,294,14,2026-06-22,05:00,07:30";
    private static final String BAD_DIRECTION_ROW =
            "39.0,-9.5,40.0,-9.0,0,1000,400,18,2026-06-22,05:00,07:30"; // direction > 360

    private final FakeWeatherDataRepository weatherRepo = new FakeWeatherDataRepository();
    private final FakeAirControlAreaRepository areaRepo = new FakeAirControlAreaRepository();

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void setUp() {
        AuthzTestHelper.loginAsWeatherPerson();
        areaRepo.seedArea(AREA, "Lisbon TMA", 38.0, -10.0, 40.0, -8.0);
    }

    @After
    public void tearDown() {
        AuthzTestHelper.clearSession();
    }

    private ImportBulkWeatherDataController controllerWith(final WeatherDataSourceReaderFactory factory) {
        return new ImportBulkWeatherDataController(weatherRepo, areaRepo, factory, null);
    }

    private ImportBulkWeatherDataController csvController() {
        return controllerWith(new WeatherDataSourceReaderFactory());
    }

    private static Reader csv(final String... rows) {
        final StringBuilder sb = new StringBuilder(HEADER);
        for (final String row : rows) {
            sb.append('\n').append(row);
        }
        return new StringReader(sb.toString());
    }

    @Test
    public void ensureImportPersistsAllRecordsForValidSource() {
        final ImportReport report = csvController()
                .importFrom(AREA, SourceType.CSV, SOURCE, csv(VALID_ROW_1, VALID_ROW_2));

        assertEquals(2, report.imported());
        assertEquals(0, report.skipped());
        assertEquals(2, weatherRepo.count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportRejectsUnknownArea() {
        csvController().importFrom("ZZZZ", SourceType.CSV, SOURCE, csv(VALID_ROW_1));
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureImportRequiresWeatherPersonRole() {
        AuthzTestHelper.loginAsPilot();
        csvController().importFrom(AREA, SourceType.CSV, SOURCE, csv(VALID_ROW_1));
    }

    @Test
    public void ensureImportSkipsInvalidRowsAndKeepsValidOnes() {
        final ImportReport report = csvController()
                .importFrom(AREA, SourceType.CSV, SOURCE, csv(VALID_ROW_1, BAD_DIRECTION_ROW, VALID_ROW_2));

        assertEquals(2, report.imported());
        assertEquals(1, report.skipped());
        assertEquals(2, weatherRepo.count());
    }

    @Test
    public void ensureImportPersistsNothingWhenEveryRowIsInvalid() {
        final ImportReport report = csvController()
                .importFrom(AREA, SourceType.CSV, SOURCE, csv(BAD_DIRECTION_ROW, BAD_DIRECTION_ROW));

        assertEquals(0, report.imported());
        assertEquals(2, report.skipped());
        assertEquals(0, weatherRepo.count());
    }

    @Test
    public void ensureControllerImportsThroughAnyReaderStrategy() {
        final WeatherDataSourceReader stub = anySource -> List.of(
                reading(39.0, -10.0, 40.0, -9.5, 2),
                reading(38.0, -10.0, 39.0, -9.5, 3));
        final Map<SourceType, Supplier<WeatherDataSourceReader>> registry =
                Map.of(SourceType.CSV, () -> stub);

        final ImportReport report = controllerWith(new WeatherDataSourceReaderFactory(registry))
                .importFrom(AREA, SourceType.CSV, SOURCE, new StringReader(""));

        assertEquals(2, report.imported());
        assertEquals(2, weatherRepo.count());
    }

    @Test
    public void ensureControllerImportsThroughOpenMeteoStrategy() {
        // a second concrete behind the same port; no controller change, no network
        final WeatherDataSourceReader openMeteo = request -> List.of(
                reading(38.0, -10.0, 39.0, -9.5, 1),
                reading(39.0, -10.0, 40.0, -9.5, 2));
        final Map<SourceType, Supplier<WeatherDataSourceReader>> registry =
                Map.of(SourceType.OPEN_METEO, () -> openMeteo);

        final ImportReport report = controllerWith(new WeatherDataSourceReaderFactory(registry))
                .importFrom(AREA, SourceType.OPEN_METEO, "Open-Meteo",
                        LocalDate.of(2026, 6, 22), LocalTime.of(5, 0), LocalTime.of(7, 30));

        assertEquals(2, report.imported());
        assertEquals(2, weatherRepo.count());
    }

    @Test
    public void ensureOverlappingForecastsFromDifferentSourcesCoexist() {
        csvController().importFrom(AREA, SourceType.CSV, "Happy Weather", csv(VALID_ROW_1));
        csvController().importFrom(AREA, SourceType.CSV, "Cloudy Weather", csv(VALID_ROW_1));

        assertEquals(2, weatherRepo.count());
    }

    @Test
    public void ensureBatchRunsInsideASingleTransaction() {
        final FakeTransactionalContext tx = new FakeTransactionalContext();
        final ImportBulkWeatherDataController controller = new ImportBulkWeatherDataController(
                weatherRepo, areaRepo, new WeatherDataSourceReaderFactory(), tx);

        final ImportReport report = controller.importFrom(AREA, SourceType.CSV, SOURCE, csv(VALID_ROW_1, VALID_ROW_2));

        assertEquals(2, report.imported());
        assertTrue("transaction should be opened", tx.began);
        assertTrue("transaction should be committed", tx.committed);
    }

    @Test(expected = UncheckedIOException.class)
    public void ensureReaderIoErrorIsReportedAsUnchecked() {
        final WeatherDataSourceReader failing = source -> {
            throw new IOException("stream broke");
        };
        controllerWith(new WeatherDataSourceReaderFactory(Map.of(SourceType.CSV, () -> failing)))
                .importFrom(AREA, SourceType.CSV, SOURCE, new StringReader(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureImportRejectsBlankDataSource() {
        csvController().importFrom(AREA, SourceType.CSV, "   ", csv(VALID_ROW_1));
    }

    @Test
    public void ensureListAirControlAreasReturnsRegisteredAreas() {
        assertNotNull(csvController().listAirControlAreas());
        assertEquals(1, count(csvController().listAirControlAreas()));
    }

    private static long count(final Iterable<?> it) {
        long n = 0;
        for (@SuppressWarnings("unused") final Object o : it) {
            n++;
        }
        return n;
    }

    @Test
    public void ensureImportsProvidedSampleFileEndToEnd() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/weather/Lisbon_WeatherData_v0.csv");
                Reader source = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            final ImportReport report = csvController()
                    .importFrom(AREA, SourceType.CSV, SOURCE, source);

            assertEquals(48, report.imported());
            assertEquals(0, report.skipped());
            assertEquals(48, weatherRepo.count());
        }
    }

    private static RawWeatherReading reading(final double swLat, final double swLon,
            final double neLat, final double neLon, final int line) {
        return new RawWeatherReading(swLat, swLon, neLat, neLon, 0.0, 1000.0, 290.0, 12.0,
                LocalDateTime.of(2026, 6, 22, 5, 0), LocalDateTime.of(2026, 6, 22, 7, 30), line);
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private static class FakeTransactionalContext implements TransactionalContext {
        private boolean began;
        private boolean committed;

        @Override
        public void beginTransaction() {
            this.began = true;
        }

        @Override
        public void commit() {
            this.committed = true;
        }

        @Override
        public void rollback() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        @Override
        public boolean isActive() {
            return this.began && !this.committed;
        }
    }


    private static class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

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
                final double swLat, final double swLon, final double neLat, final double neLon) {
            final AirControlArea area = new AirControlArea(
                    new AreaCode(code), new AreaName(name),
                    new GeographicBoundary(new Coordinates(swLat, swLon), new Coordinates(neLat, neLon)));
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
            return data.values().stream().filter(a -> a.name().equals(name)).findFirst();
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