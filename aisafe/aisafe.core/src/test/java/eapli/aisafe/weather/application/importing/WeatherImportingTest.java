package eapli.aisafe.weather.application.importing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.weather.application.importing.csv.CsvWeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.openmeteo.OpenMeteoWeatherDataSourceReader;

public class WeatherImportingTest {

    private static RawWeatherReading sampleReading() {
        return new RawWeatherReading(39.0, -10.0, 40.0, -9.5, 0.0, 1000.0, 290.0, 12.0,
                LocalDateTime.of(2026, 6, 22, 5, 0), LocalDateTime.of(2026, 6, 22, 7, 30), 2);
    }

    @Test
    public void ensureRawWeatherReadingExposesEveryField() {
        final RawWeatherReading r = sampleReading();
        assertEquals(39.0, r.swLat(), 0.0);
        assertEquals(-10.0, r.swLon(), 0.0);
        assertEquals(40.0, r.neLat(), 0.0);
        assertEquals(-9.5, r.neLon(), 0.0);
        assertEquals(0.0, r.altLowerFeet(), 0.0);
        assertEquals(1000.0, r.altUpperFeet(), 0.0);
        assertEquals(290.0, r.windDirectionDegrees(), 0.0);
        assertEquals(12.0, r.windSpeedKnots(), 0.0);
        assertEquals(LocalDateTime.of(2026, 6, 22, 5, 0), r.start());
        assertEquals(LocalDateTime.of(2026, 6, 22, 7, 30), r.end());
        assertEquals(2, r.sourceLine());
        assertNotNull(r.toString());
    }

    @Test
    public void ensureImportReportCountsImportedAndSkipped() {
        final List<RowError> errors = List.of(new RowError(5, "bad"), new RowError(8, "worse"));
        final ImportReport report = new ImportReport(10, errors);
        assertEquals(10, report.imported());
        assertEquals(2, report.skipped());
        assertEquals(2, report.errors().size());
        assertNotNull(report.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureImportReportErrorsAreUnmodifiable() {
        new ImportReport(0, List.of()).errors().add(new RowError(1, "x"));
    }

    @Test
    public void ensureRowErrorExposesLineAndReason() {
        final RowError error = new RowError(7, "wind direction out of range");
        assertEquals(7, error.line());
        assertEquals("wind direction out of range", error.reason());
        assertTrue(error.toString().contains("7"));
    }

    @Test
    public void ensureSourceTypeHasCsvAndOpenMeteo() {
        assertEquals(SourceType.CSV, SourceType.valueOf("CSV"));
        assertEquals(SourceType.OPEN_METEO, SourceType.valueOf("OPEN_METEO"));
        assertEquals(2, SourceType.values().length);
    }

    @Test
    public void ensureFactoryReturnsCsvReaderByDefault() {
        assertTrue(new WeatherDataSourceReaderFactory().readerFor(SourceType.CSV)
                instanceof CsvWeatherDataSourceReader);
    }

    @Test
    public void ensureFactoryReturnsOpenMeteoReaderByDefault() {
        assertTrue(new WeatherDataSourceReaderFactory().readerFor(SourceType.OPEN_METEO)
                instanceof OpenMeteoWeatherDataSourceReader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFactoryRejectsNullSourceType() {
        new WeatherDataSourceReaderFactory().readerFor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureFactoryRejectsUnregisteredSourceType() {
        new WeatherDataSourceReaderFactory(Map.of()).readerFor(SourceType.CSV);
    }

    @Test
    public void ensureFactoryUsesCustomRegistry() {
        final WeatherDataSourceReader stub = source -> List.of();
        final WeatherDataSourceReader resolved =
                new WeatherDataSourceReaderFactory(Map.of(SourceType.CSV, () -> stub))
                        .readerFor(SourceType.CSV);
        assertEquals(stub, resolved);
    }
}