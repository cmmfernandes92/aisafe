package eapli.aisafe.weather.application.importing.openmeteo;

import java.io.IOException;
import java.net.URI;

/**
 * The single point of network access for the Open-Meteo adapter. Abstracting the
 * HTTP call behind this seam lets {@link OpenMeteoWeatherDataSourceReader} be
 * unit-tested on canned JSON with no network, and keeps the JDK HTTP client out
 * of the adapter's logic.
 */
@FunctionalInterface
public interface OpenMeteoClient {

    /**
     * Performs a GET and returns the response body.
     *
     * @param uri the fully-built Open-Meteo forecast URL
     * @return the raw JSON body
     * @throws IOException if the request fails or returns a non-success status
     */
    String fetch(URI uri) throws IOException;
}