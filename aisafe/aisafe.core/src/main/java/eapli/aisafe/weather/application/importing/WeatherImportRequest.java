package eapli.aisafe.weather.application.importing;

import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;

/**
 * What a {@link WeatherDataSourceReader} is given to read. It carries every input
 * a source could need so that a file reader and an API adapter satisfy one port:
 * a file reader uses the {@link #file()}; an API adapter uses the
 * {@link #areaBounds() area boundary} and the operator's {@link #day()} /
 * {@link #start()} / {@link #end()} window. Fields irrelevant to a given source
 * are absent (a file source has no window; an API source has no file). The
 * controller assembles the request, attaching the resolved area's boundary.
 */
public final class WeatherImportRequest {

    private final Reader file;
    private final GeographicBoundary areaBounds;
    private final LocalDate day;
    private final LocalTime start;
    private final LocalTime end;

    public WeatherImportRequest(final Reader file, final GeographicBoundary areaBounds,
            final LocalDate day, final LocalTime start, final LocalTime end) {
        this.file = file;
        this.areaBounds = areaBounds;
        this.day = day;
        this.start = start;
        this.end = end;
    }

    /**
     * A file-backed request (e.g. CSV). The area boundary and time window are not
     * used by file readers, which carry the period in the file itself.
     */
    public static WeatherImportRequest ofFile(final Reader file) {
        return new WeatherImportRequest(file, null, null, null, null);
    }

    /** An API-backed request: the area box and the operator's day/time window. */
    public static WeatherImportRequest ofApi(final GeographicBoundary areaBounds,
            final LocalDate day, final LocalTime start, final LocalTime end) {
        return new WeatherImportRequest(null, areaBounds, day, start, end);
    }

    /** The character stream of a file source, if any. */
    public Optional<Reader> file() {
        return Optional.ofNullable(this.file);
    }

    /** The target area's bounding box; required by API sources, ignored by file sources. */
    public GeographicBoundary areaBounds() {
        return this.areaBounds;
    }

    public LocalDate day() {
        return this.day;
    }

    public LocalTime start() {
        return this.start;
    }

    public LocalTime end() {
        return this.end;
    }
}