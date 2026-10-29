package eapli.aisafe.weather.application.importing.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.WeatherImportRequest;

public class OpenMeteoWeatherDataSourceReaderTest {

    private static final int BANDS = 6;

    /** Single hour, all six mapped pressure levels present, distinct values per level. */
    private static final String CANNED_JSON = "{\"hourly\":{"
            + "\"time\":[\"2026-06-22T05:00\"],"
            + "\"wind_speed_1000hPa\":[10.0],\"wind_direction_1000hPa\":[180.0],"
            + "\"wind_speed_950hPa\":[12.0],\"wind_direction_950hPa\":[190.0],"
            + "\"wind_speed_925hPa\":[14.0],\"wind_direction_925hPa\":[200.0],"
            + "\"wind_speed_900hPa\":[16.0],\"wind_direction_900hPa\":[210.0],"
            + "\"wind_speed_800hPa\":[18.0],\"wind_direction_800hPa\":[220.0],"
            + "\"wind_speed_700hPa\":[20.0],\"wind_direction_700hPa\":[230.0]"
            + "}}";

    private static GeographicBoundary box(final double minLat, final double minLon,
            final double maxLat, final double maxLon) {
        return new GeographicBoundary(new Coordinates(minLat, minLon), new Coordinates(maxLat, maxLon));
    }

    private static WeatherImportRequest request(final GeographicBoundary bounds) {
        return WeatherImportRequest.ofApi(bounds,
                LocalDate.of(2026, 6, 22), LocalTime.of(5, 0), LocalTime.of(7, 30));
    }

    private static OpenMeteoWeatherDataSourceReader readerReturning(final String json) {
        return new OpenMeteoWeatherDataSourceReader(uri -> json);
    }

    @Test
    public void ensureGridsLisbonBoxIntoEightCellsTimesSixBands() throws IOException {
        // 2.0 deg x 2.0 deg over Lisbon -> ceil(2/1.0)=2 rows, ceil(2/0.5)=4 cols = 8 cells
        final List<RawWeatherReading> readings =
                readerReturning(CANNED_JSON).read(request(box(38.0, -10.0, 40.0, -8.0)));

        assertEquals(8 * BANDS, readings.size());
    }

    @Test
    public void ensureCellCountAdaptsToAreaButCellSizeIsFixed() throws IOException {
        // a single 1.0 deg x 0.5 deg cell
        final List<RawWeatherReading> readings =
                readerReturning(CANNED_JSON).read(request(box(38.0, -10.0, 39.0, -9.5)));

        assertEquals(BANDS, readings.size());
    }

    @Test
    public void ensureNormalizesWindAndBandsFromTheResponse() throws IOException {
        final List<RawWeatherReading> readings =
                readerReturning(CANNED_JSON).read(request(box(38.0, -10.0, 39.0, -9.5)));

        // first band (0-1000 ft, mid 500) maps to the 1000 hPa level
        final RawWeatherReading low = readings.get(0);
        assertEquals(0.0, low.altLowerFeet(), 0.0);
        assertEquals(1000.0, low.altUpperFeet(), 0.0);
        assertEquals(10.0, low.windSpeedKnots(), 0.0);
        assertEquals(180.0, low.windDirectionDegrees(), 0.0);

        // top band (7001-12000 ft, mid 9500) maps to the 700 hPa level
        final RawWeatherReading high = readings.get(BANDS - 1);
        assertEquals(7001.0, high.altLowerFeet(), 0.0);
        assertEquals(12000.0, high.altUpperFeet(), 0.0);
        assertEquals(20.0, high.windSpeedKnots(), 0.0);
        assertEquals(230.0, high.windDirectionDegrees(), 0.0);
    }

    @Test
    public void ensureEverySampleSitsInsideTheAreaBox() throws IOException {
        final GeographicBoundary bounds = box(38.0, -10.0, 40.0, -8.0);
        final List<RawWeatherReading> readings = readerReturning(CANNED_JSON).read(request(bounds));

        for (final RawWeatherReading r : readings) {
            final double midLat = (r.swLat() + r.neLat()) / 2.0;
            final double midLon = (r.swLon() + r.neLon()) / 2.0;
            assertTrue(bounds.contains(new Coordinates(midLat, midLon)));
        }
    }

    @Test
    public void ensureBandIsSkippedWhenProviderValueIsMissing() throws IOException {
        // drop the 700 hPa speed (top band's level) -> that band is not emitted
        final String json = CANNED_JSON.replace("\"wind_speed_700hPa\":[20.0]", "\"wind_speed_700hPa\":[null]");

        final List<RawWeatherReading> readings =
                readerReturning(json).read(request(box(38.0, -10.0, 39.0, -9.5)));

        assertEquals(BANDS - 1, readings.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsRequestWithoutWindow() throws IOException {
        readerReturning(CANNED_JSON).read(WeatherImportRequest.ofFile(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsNullClient() {
        new OpenMeteoWeatherDataSourceReader(null);
    }

    @Test
    public void ensureNoArgConstructorBuildsLiveClientBackedReader() {
        assertNotNull(new OpenMeteoWeatherDataSourceReader());
    }

    // ── Malformed / untrusted responses: a band is skipped, never mis-imported ──

    private List<RawWeatherReading> readSingleCell(final String json) throws IOException {
        return readerReturning(json).read(request(box(38.0, -10.0, 39.0, -9.5)));
    }

    @Test
    public void ensureSkipsBandsWhoseLevelArrayIsAbsent() throws IOException {
        // only the 1000 hPa level present -> only the lowest band is emitted
        final String json = "{\"hourly\":{\"time\":[\"2026-06-22T05:00\"],"
                + "\"wind_speed_1000hPa\":[10.0],\"wind_direction_1000hPa\":[180.0]}}";
        assertEquals(1, readSingleCell(json).size());
    }

    @Test
    public void ensureSkipsBandWhenValueIsNonNumeric() throws IOException {
        final String json = "{\"hourly\":{\"time\":[\"2026-06-22T05:00\"],"
                + "\"wind_speed_1000hPa\":[\"abc\"],\"wind_direction_1000hPa\":[180.0]}}";
        assertEquals(0, readSingleCell(json).size());
    }

    @Test
    public void ensureSkipsBandWhenValueIndexIsOutOfRange() throws IOException {
        // ask for 06:00 (index 1) but the value arrays hold a single element
        final String json = "{\"hourly\":{\"time\":[\"2026-06-09T05:00\",\"2026-06-09T06:00\"],"
                + "\"wind_speed_1000hPa\":[10.0],\"wind_direction_1000hPa\":[180.0]}}";
        final WeatherImportRequest req = WeatherImportRequest.ofApi(box(38.0, -10.0, 39.0, -9.5),
                LocalDate.of(2026, 6, 9), LocalTime.of(6, 0), LocalTime.of(7, 0));
        assertEquals(0, readerReturning(json).read(req).size());
    }

    @Test
    public void ensureToleratesUnterminatedHourlyObject() throws IOException {
        // missing closing brace -> the hourly scope falls back to the remainder
        final String json = "{\"hourly\":{\"time\":[\"2026-06-22T05:00\"],"
                + "\"wind_speed_1000hPa\":[10.0],\"wind_direction_1000hPa\":[180.0]";
        assertEquals(1, readSingleCell(json).size());
    }

    @Test
    public void ensureSkipsBandWhenArrayIsUnterminated() throws IOException {
        final String json = "{\"hourly\":{\"time\":[\"2026-06-22T05:00\"],"
                + "\"wind_direction_1000hPa\":[180.0],\"wind_speed_1000hPa\":[10.0}}";
        assertEquals(0, readSingleCell(json).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsResponseWithoutTimeArray() throws IOException {
        readSingleCell("{\"hourly\":{\"wind_speed_1000hPa\":[10.0]}}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsResponseWithoutHourlyObject() throws IOException {
        readSingleCell("{\"foo\":1}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsHourlyThatIsNotAnObject() throws IOException {
        readSingleCell("{\"hourly\":\"oops\"}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsResponseMissingRequestedHour() throws IOException {
        final String json = CANNED_JSON.replace("2026-06-22T05:00", "2026-06-22T09:00");
        readerReturning(json).read(request(box(38.0, -10.0, 39.0, -9.5)));
    }

    @Test
    public void ensureParsesRealOpenMeteoResponseShape() throws IOException {
        // A captured live response (lat 38.5, lon -9.75, 2026-06-09) — it has the
        // "hourly_units" block that a naive whole-document scan would mis-match.
        final String real = resource("/weather/openmeteo_sample_response.json");
        final WeatherImportRequest request = WeatherImportRequest.ofApi(
                box(38.0, -10.0, 39.0, -9.5), // single cell, centroid 38.5,-9.75
                LocalDate.of(2026, 6, 9), LocalTime.of(5, 0), LocalTime.of(7, 30));

        final List<RawWeatherReading> readings =
                new OpenMeteoWeatherDataSourceReader(uri -> real).read(request);

        assertEquals(BANDS, readings.size()); // all six bands resolved to finite wind
        for (final RawWeatherReading r : readings) {
            assertTrue(r.windSpeedKnots() >= 0);
            assertTrue(r.windDirectionDegrees() >= 0 && r.windDirectionDegrees() <= 360);
        }
    }

    private static String resource(final String path) throws IOException {
        try (InputStream in = OpenMeteoWeatherDataSourceReaderTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test(expected = IOException.class)
    public void ensurePropagatesClientIoError() throws IOException {
        final OpenMeteoWeatherDataSourceReader reader = new OpenMeteoWeatherDataSourceReader(uri -> {
            throw new IOException("network down");
        });
        reader.read(request(box(38.0, -10.0, 39.0, -9.5)));
    }
}