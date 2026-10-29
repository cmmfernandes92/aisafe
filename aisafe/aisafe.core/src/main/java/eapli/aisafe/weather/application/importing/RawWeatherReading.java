package eapli.aisafe.weather.application.importing;

import java.time.LocalDateTime;

/**
 * One parsed row from a weather data source, in canonical units (degrees, feet,
 * knots, local date/time). It is a plain data carrier produced by a
 * {@link WeatherDataSourceReader}: it holds the values as read and enforces no
 * domain invariant. The target air control area and the data source (provider)
 * are not part of a reading; they are supplied at import time and attributed by
 * the controller when it assembles a {@code WeatherData} record.
 * <p>
 * The southwest / northeast corners are carried exactly as labelled in the
 * source; whether they really are SW and NE is validated by the
 * {@code WeatherZone} value object during assembly.
 */
public final class RawWeatherReading {

    private final double swLat;
    private final double swLon;
    private final double neLat;
    private final double neLon;
    private final double altLowerFeet;
    private final double altUpperFeet;
    private final double windDirectionDegrees;
    private final double windSpeedKnots;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final int sourceLine;

    public RawWeatherReading(final double swLat, final double swLon,
            final double neLat, final double neLon,
            final double altLowerFeet, final double altUpperFeet,
            final double windDirectionDegrees, final double windSpeedKnots,
            final LocalDateTime start, final LocalDateTime end,
            final int sourceLine) {
        this.swLat = swLat;
        this.swLon = swLon;
        this.neLat = neLat;
        this.neLon = neLon;
        this.altLowerFeet = altLowerFeet;
        this.altUpperFeet = altUpperFeet;
        this.windDirectionDegrees = windDirectionDegrees;
        this.windSpeedKnots = windSpeedKnots;
        this.start = start;
        this.end = end;
        this.sourceLine = sourceLine;
    }

    public double swLat() {
        return this.swLat;
    }

    public double swLon() {
        return this.swLon;
    }

    public double neLat() {
        return this.neLat;
    }

    public double neLon() {
        return this.neLon;
    }

    public double altLowerFeet() {
        return this.altLowerFeet;
    }

    public double altUpperFeet() {
        return this.altUpperFeet;
    }

    public double windDirectionDegrees() {
        return this.windDirectionDegrees;
    }

    public double windSpeedKnots() {
        return this.windSpeedKnots;
    }

    public LocalDateTime start() {
        return this.start;
    }

    public LocalDateTime end() {
        return this.end;
    }

    /** 1-based line number of this row in the source, for error reporting. */
    public int sourceLine() {
        return this.sourceLine;
    }

    @Override
    public String toString() {
        return "RawWeatherReading[line=" + sourceLine
                + ", sw=(" + swLat + "," + swLon + "), ne=(" + neLat + "," + neLon + ")"
                + ", alt=" + altLowerFeet + "-" + altUpperFeet + "ft"
                + ", dir=" + windDirectionDegrees + ", spd=" + windSpeedKnots
                + ", " + start + ".." + end + "]";
    }
}