package eapli.aisafe.weather.application.importing.csv;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.WeatherImportRequest;

public class CsvWeatherDataSourceReaderTest {

    private static final String HEADER =
            "SW Lat,SW Lon,NE Lat,NE Lon,Alt_inf (ft),Alt_Sup (ft),Direction (degree),Value (knot),Day,Star,End";

    private final CsvWeatherDataSourceReader reader = new CsvWeatherDataSourceReader();

    private List<RawWeatherReading> read(final String csv) throws IOException {
        return reader.read(WeatherImportRequest.ofFile(new StringReader(csv)));
    }

    @Test
    public void ensureReadsAllDataRowsFromValidCsv() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n"
                + "38.0,-10.0,39.0,-9.5,0,1000,294,14,2026-06-22,05:00,07:30\n"
                + "39.0,-9.5,40.0,-9.0,1001,2000,308,26,2026-06-22,05:00,07:30\n";

        final List<RawWeatherReading> readings = read(csv);

        assertEquals(3, readings.size());
    }

    @Test
    public void ensureParsesCornersAltitudeAndWindAsLabelled() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n";

        final RawWeatherReading r = read(csv).get(0);

        assertEquals(39.0, r.swLat(), 0.0);
        assertEquals(-10.0, r.swLon(), 0.0);
        assertEquals(40.0, r.neLat(), 0.0);
        assertEquals(-9.5, r.neLon(), 0.0);
        assertEquals(0.0, r.altLowerFeet(), 0.0);
        assertEquals(1000.0, r.altUpperFeet(), 0.0);
        assertEquals(290.0, r.windDirectionDegrees(), 0.0);
        assertEquals(12.0, r.windSpeedKnots(), 0.0);
    }

    @Test
    public void ensureCombinesDayAndTimesIntoPeriod() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n";

        final RawWeatherReading r = read(csv).get(0);

        assertEquals(LocalDateTime.of(2026, 6, 22, 5, 0), r.start());
        assertEquals(LocalDateTime.of(2026, 6, 22, 7, 30), r.end());
    }

    @Test
    public void ensureReportsOneBasedSourceLine() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n"
                + "38.0,-10.0,39.0,-9.5,0,1000,294,14,2026-06-22,05:00,07:30\n";

        final List<RawWeatherReading> readings = read(csv);

        assertEquals(2, readings.get(0).sourceLine()); // first data row is line 2
        assertEquals(3, readings.get(1).sourceLine());
    }

    @Test
    public void ensureBlankLinesAreSkipped() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n"
                + "\n"
                + "   \n"
                + "38.0,-10.0,39.0,-9.5,0,1000,294,14,2026-06-22,05:00,07:30\n";

        assertEquals(2, read(csv).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsUnexpectedHeader() throws IOException {
        final String csv = "Area,Lat,Lon\n39.0,-10.0,40.0\n";
        read(csv);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsEmptySource() throws IOException {
        read("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsNullSource() throws IOException {
        reader.read(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsHeaderWithRightCountButWrongNames() throws IOException {
        final String csv =
                "A,B,C,D,E,F,G,H,I,J,K\n39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00,07:30\n";
        read(csv);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsRowWithWrongColumnCount() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,2026-06-22,05:00\n"; // 10 columns
        read(csv);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsNonNumericValue() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,WINDY,2026-06-22,05:00,07:30\n";
        read(csv);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRejectsMalformedDate() throws IOException {
        final String csv = HEADER + "\n"
                + "39.0,-10.0,40.0,-9.5,0,1000,290,12,22-06-2026,05:00,07:30\n";
        read(csv);
    }
}