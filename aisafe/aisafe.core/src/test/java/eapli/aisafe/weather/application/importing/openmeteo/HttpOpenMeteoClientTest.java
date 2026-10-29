package eapli.aisafe.weather.application.importing.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Exercises the live HTTP wrapper against an in-JVM server, so the success and
 * error paths are covered without reaching the public internet.
 */
public class HttpOpenMeteoClientTest {

    private HttpServer server;
    private int port;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ok", ex -> {
            final byte[] body = "{\"hourly\":{}}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.createContext("/boom", ex -> {
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void ensureReturnsBodyOnSuccess() throws IOException {
        final String body = new HttpOpenMeteoClient().fetch(URI.create("http://localhost:" + port + "/ok"));
        assertEquals("{\"hourly\":{}}", body);
    }

    @Test
    public void ensureFailsOnNonSuccessStatus() {
        try {
            new HttpOpenMeteoClient().fetch(URI.create("http://localhost:" + port + "/boom"));
            fail("expected IOException for HTTP 500");
        } catch (final IOException expected) {
            // status code surfaced as IOException
        }
    }
}