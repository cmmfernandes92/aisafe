package eapli.aisafe.weather.application.importing;

import java.io.IOException;
import java.util.List;

/**
 * Strategy port for reading bulk weather data from an external source into
 * canonical {@link RawWeatherReading} rows. Each concrete reader owns the format
 * concerns of one source (a CSV file, the Open-Meteo API, …) and depends on
 * nothing in the weather domain or persistence layers. New sources are added by
 * implementing this interface, without changing the import use case (Open/Closed).
 */
public interface WeatherDataSourceReader {

    /**
     * Reads the given request into readings. A file reader uses the request's
     * file; an API adapter uses the request's area boundary and time window.
     *
     * @param request the source inputs (file, or area box + day/time window)
     * @return the parsed rows, in source order
     * @throws IOException              if the underlying source cannot be read
     * @throws IllegalArgumentException if the request is missing what the reader
     *                                  needs, or the source is structurally
     *                                  invalid (e.g. unexpected header)
     */
    List<RawWeatherReading> read(WeatherImportRequest request) throws IOException;
}