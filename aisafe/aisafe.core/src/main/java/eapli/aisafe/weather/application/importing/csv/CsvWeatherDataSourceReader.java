package eapli.aisafe.weather.application.importing.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.WeatherImportRequest;

/**
 * Reads the project's CSV weather format into {@link RawWeatherReading} rows.
 * <p>
 * Expected columns (the air control area and the data source are supplied at
 * import time, not in the file):
 *
 * <pre>
 * SW Lat,SW Lon,NE Lat,NE Lon,Alt_inf (ft),Alt_Sup (ft),Direction (degree),Value (knot),Day,Star,End
 * </pre>
 *
 * {@code Day} is an ISO date ({@code yyyy-MM-dd}); {@code Star}/{@code End} are
 * ISO times ({@code HH:mm}) and are combined with {@code Day} into the period
 * start and end. The reader validates structure only (header and column count);
 * domain invariants are enforced later, when the controller builds the value
 * objects.
 */
public class CsvWeatherDataSourceReader implements WeatherDataSourceReader {

    private static final String SEPARATOR = ",";

    private static final String[] EXPECTED_HEADER = {
            "SW Lat", "SW Lon", "NE Lat", "NE Lon",
            "Alt_inf (ft)", "Alt_Sup (ft)",
            "Direction (degree)", "Value (knot)",
            "Day", "Star", "End"
    };
    private static final int COLUMN_COUNT = EXPECTED_HEADER.length;

    @Override
    public List<RawWeatherReading> read(final WeatherImportRequest request) throws IOException {
        if (request == null || request.file().isEmpty()) {
            throw new IllegalArgumentException("CSV import requires a file source");
        }
        final Reader source = request.file().get();

        final List<RawWeatherReading> readings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(source)) {
            final String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV source is empty");
            }
            validateHeader(headerLine);

            String line;
            int lineNumber = 1; // header consumed as line 1
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                readings.add(parseRow(line, lineNumber));
            }
        }
        return readings;
    }

    private void validateHeader(final String headerLine) {
        final String[] columns = headerLine.split(SEPARATOR, -1);
        if (columns.length != COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Unexpected header: expected " + COLUMN_COUNT + " columns but found " + columns.length);
        }
        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (!columns[i].trim().equalsIgnoreCase(EXPECTED_HEADER[i])) {
                throw new IllegalArgumentException(
                        "Unexpected header column " + (i + 1) + ": expected '" + EXPECTED_HEADER[i]
                                + "' but found '" + columns[i].trim() + "'");
            }
        }
    }

    private RawWeatherReading parseRow(final String line, final int lineNumber) {
        final String[] f = line.split(SEPARATOR, -1);
        if (f.length != COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Line " + lineNumber + ": expected " + COLUMN_COUNT + " columns but found " + f.length);
        }
        try {
            final double swLat = Double.parseDouble(f[0].trim());
            final double swLon = Double.parseDouble(f[1].trim());
            final double neLat = Double.parseDouble(f[2].trim());
            final double neLon = Double.parseDouble(f[3].trim());
            final double altLower = Double.parseDouble(f[4].trim());
            final double altUpper = Double.parseDouble(f[5].trim());
            final double direction = Double.parseDouble(f[6].trim());
            final double speed = Double.parseDouble(f[7].trim());

            final LocalDate day = LocalDate.parse(f[8].trim());
            final LocalDateTime start = LocalDateTime.of(day, LocalTime.parse(f[9].trim()));
            final LocalDateTime end = LocalDateTime.of(day, LocalTime.parse(f[10].trim()));

            return new RawWeatherReading(swLat, swLon, neLat, neLon,
                    altLower, altUpper, direction, speed, start, end, lineNumber);
        } catch (final NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage(), e);
        }
    }
}