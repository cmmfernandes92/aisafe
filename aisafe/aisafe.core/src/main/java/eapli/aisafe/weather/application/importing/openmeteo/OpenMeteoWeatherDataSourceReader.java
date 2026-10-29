package eapli.aisafe.weather.application.importing.openmeteo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.weather.application.importing.RawWeatherReading;
import eapli.aisafe.weather.application.importing.WeatherDataSourceReader;
import eapli.aisafe.weather.application.importing.WeatherImportRequest;

/**
 * Reads bulk weather from the Open-Meteo API (keyless). It is a Strategy
 * implementation that is also an Adapter: it wraps an external service behind the
 * {@link WeatherDataSourceReader} port and emits the same {@link RawWeatherReading}
 * rows the CSV reader produces, so the controller, domain and CSV reader are
 * unchanged.
 * <p>
 * The provider returns point values, so the adapter imposes a fixed-resolution
 * grid over the area boundary, samples each cell at its centroid, maps each fixed
 * altitude band to the nearest pressure level, and normalises the response. These
 * spatial / vertical / temporal conversions are lossy and isolated here (a
 * documented MVP simplification); the domain re-validates every value on arrival.
 */
public class OpenMeteoWeatherDataSourceReader implements WeatherDataSourceReader {

    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast";

    /** Fixed resolution derived from the provided Lisbon sample (1.0 deg x 0.5 deg). */
    private static final double CELL_LAT = 1.0;
    private static final double CELL_LON = 0.5;

    /** Fixed altitude bands in feet, lower / upper. */
    private static final double[][] BANDS = {
            {0, 1000}, {1001, 2000}, {2001, 3450}, {3451, 4500}, {4501, 7000}, {7001, 12000}
    };

    /** Pressure levels (hPa) and their approximate height above sea level, in feet. */
    private static final int[] LEVELS_HPA = {1000, 975, 950, 925, 900, 850, 800, 700, 600, 500};
    private static final double[] LEVELS_FEET = {361, 1050, 1640, 2625, 3281, 4921, 6234, 9843, 13780, 18373};

    private final OpenMeteoClient client;

    /** Production constructor: live HTTP access. */
    public OpenMeteoWeatherDataSourceReader() {
        this(new HttpOpenMeteoClient());
    }

    /** Test/extension constructor: any client (e.g. a canned-JSON stub). */
    public OpenMeteoWeatherDataSourceReader(final OpenMeteoClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Open-Meteo client must not be null");
        }
        this.client = client;
    }

    @Override
    public List<RawWeatherReading> read(final WeatherImportRequest request) throws IOException {
        if (request == null || request.areaBounds() == null
                || request.day() == null || request.start() == null || request.end() == null) {
            throw new IllegalArgumentException(
                    "Open-Meteo import requires an area boundary and a day/time window");
        }

        final GeographicBoundary bounds = request.areaBounds();
        final LocalDate day = request.day();
        final LocalDateTime start = LocalDateTime.of(day, request.start());
        final LocalDateTime end = LocalDateTime.of(day, request.end());

        final int[] bandLevels = new int[BANDS.length];
        final Set<Integer> distinctLevels = new LinkedHashSet<>();
        for (int b = 0; b < BANDS.length; b++) {
            bandLevels[b] = nearestLevel((BANDS[b][0] + BANDS[b][1]) / 2.0);
            distinctLevels.add(bandLevels[b]);
        }

        final double minLat = bounds.southwest().latitude();
        final double minLon = bounds.southwest().longitude();
        final double maxLat = bounds.northeast().latitude();
        final double maxLon = bounds.northeast().longitude();
        final int rows = (int) Math.ceil((maxLat - minLat) / CELL_LAT);
        final int cols = (int) Math.ceil((maxLon - minLon) / CELL_LON);

        final List<RawWeatherReading> readings = new ArrayList<>();
        int line = 0;
        for (int r = 0; r < rows; r++) {
            final double cellMinLat = minLat + r * CELL_LAT;
            final double cellMaxLat = Math.min(cellMinLat + CELL_LAT, maxLat);
            for (int c = 0; c < cols; c++) {
                final double cellMinLon = minLon + c * CELL_LON;
                final double cellMaxLon = Math.min(cellMinLon + CELL_LON, maxLon);
                final double centroidLat = (cellMinLat + cellMaxLat) / 2.0;
                final double centroidLon = (cellMinLon + cellMaxLon) / 2.0;

                final String json = client.fetch(buildUri(centroidLat, centroidLon, day, distinctLevels));
                // Scope parsing to the "hourly" object: the response also has an
                // "hourly_units" object whose keys (time, wind_speed_1000hPa, …) carry
                // string values, which a whole-document scan would match by mistake.
                final String hourly = hourlyObject(json);
                final List<String> time = stringArray(hourly, "time");
                final int idx = hourIndex(time, day, request.start());

                for (int b = 0; b < BANDS.length; b++) {
                    final int level = bandLevels[b];
                    final double speed = valueAt(hourly, "wind_speed_" + level + "hPa", idx);
                    final double direction = valueAt(hourly, "wind_direction_" + level + "hPa", idx);
                    if (!Double.isFinite(speed) || !Double.isFinite(direction)) {
                        continue; // provider gap at this level; skip rather than emit junk
                    }
                    readings.add(new RawWeatherReading(
                            cellMinLat, cellMinLon, cellMaxLat, cellMaxLon,
                            BANDS[b][0], BANDS[b][1], direction, speed, start, end, ++line));
                }
            }
        }
        return readings;
    }

    private static int nearestLevel(final double bandMidFeet) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < LEVELS_HPA.length; i++) {
            final double dist = Math.abs(LEVELS_FEET[i] - bandMidFeet);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return LEVELS_HPA[best];
    }

    private static URI buildUri(final double lat, final double lon, final LocalDate day,
            final Set<Integer> levels) {
        final StringBuilder hourly = new StringBuilder();
        for (final int level : levels) {
            if (hourly.length() > 0) {
                hourly.append(',');
            }
            hourly.append("wind_speed_").append(level).append("hPa,")
                    .append("wind_direction_").append(level).append("hPa");
        }
        final String query = "latitude=" + lat
                + "&longitude=" + lon
                + "&start_date=" + day
                + "&end_date=" + day
                + "&wind_speed_unit=kn"
                + "&hourly=" + URLEncoder.encode(hourly.toString(), StandardCharsets.UTF_8);
        return URI.create(ENDPOINT + "?" + query);
    }

    /** Index in the hourly {@code time} array matching the window's start hour. */
    private static int hourIndex(final List<String> time, final LocalDate day, final LocalTime start) {
        final String prefix = day + "T" + String.format("%02d:", start.getHour());
        for (int i = 0; i < time.size(); i++) {
            if (time.get(i).startsWith(prefix)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Open-Meteo response has no hour matching " + day + " " + start);
    }

    /** Reads the numeric value of {@code key}'s array at {@code index} ({@code NaN} if absent/null). */
    private static double valueAt(final String json, final String key, final int index) {
        final String body = arrayBody(json, key);
        if (body == null) {
            return Double.NaN;
        }
        final String[] cells = body.split(",");
        if (index < 0 || index >= cells.length) {
            return Double.NaN;
        }
        final String cell = cells[index].trim();
        if (cell.isEmpty() || "null".equalsIgnoreCase(cell)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(cell);
        } catch (final NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static List<String> stringArray(final String json, final String key) {
        final List<String> values = new ArrayList<>();
        final String body = arrayBody(json, key);
        if (body == null || body.isBlank()) {
            return values;
        }
        for (final String cell : body.split(",")) {
            values.add(cell.trim().replace("\"", ""));
        }
        return values;
    }

    /**
     * The body of the top-level {@code "hourly"} object (the brace-delimited text
     * after {@code "hourly":}). Anchored on {@code "hourly"} with its closing quote,
     * so it does not match {@code "hourly_units"}.
     */
    private static String hourlyObject(final String json) {
        final int h = json.indexOf("\"hourly\"");
        if (h < 0) {
            return json;
        }
        final int open = json.indexOf('{', h);
        if (open < 0) {
            return json;
        }
        int depth = 0;
        for (int i = open; i < json.length(); i++) {
            final char ch = json.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(open, i + 1);
                }
            }
        }
        return json.substring(open);
    }

    /** The raw text between the brackets of the JSON array named {@code key}. */
    private static String arrayBody(final String json, final String key) {
        final int k = json.indexOf("\"" + key + "\"");
        if (k < 0) {
            return null;
        }
        final int open = json.indexOf('[', k);
        final int close = open < 0 ? -1 : json.indexOf(']', open);
        if (open < 0 || close < 0) {
            return null;
        }
        return json.substring(open + 1, close);
    }
}