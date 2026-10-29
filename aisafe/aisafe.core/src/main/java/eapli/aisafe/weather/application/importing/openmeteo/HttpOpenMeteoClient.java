package eapli.aisafe.weather.application.importing.openmeteo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The production {@link OpenMeteoClient}: a thin wrapper over the JDK HTTP client.
 * No third-party dependency and no API key (Open-Meteo is keyless).
 */
public class HttpOpenMeteoClient implements OpenMeteoClient {

    private static final int OK = 200;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String fetch(final URI uri) throws IOException {
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != OK) {
                throw new IOException("Open-Meteo returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Open-Meteo request was interrupted", e);
        }
    }
}