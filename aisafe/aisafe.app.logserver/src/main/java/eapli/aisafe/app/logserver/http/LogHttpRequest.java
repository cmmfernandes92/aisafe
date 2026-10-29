package eapli.aisafe.app.logserver.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import eapli.aisafe.app.logserver.store.LogStore;

/**
 * US091 — handles one HTTP request. Reads the request line, drains the headers,
 * then routes:
 *
 * <ul>
 *   <li>{@code GET /events} → HTML fragment of the last events (AJAX region)</li>
 *   <li>{@code GET /active} → HTML fragment of the active remote users (AJAX region)</li>
 *   <li>{@code GET /} or {@code /index.html} and other static assets → served from {@code /www}</li>
 *   <li>anything else → {@code 404}; non-GET → {@code 405}</li>
 * </ul>
 */
public final class LogHttpRequest implements Runnable {

    private static final String HTML = "text/html; charset=utf-8";

    private final Socket socket;
    private final LogStore store;

    public LogHttpRequest(final Socket socket, final LogStore store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try (Socket s = socket;
                InputStream in = new BufferedInputStream(s.getInputStream());
                OutputStream out = s.getOutputStream()) {

            final String requestLine = Http.readLineCRLF(in);
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            drainHeaders(in);

            final String[] parts = requestLine.split(" ");
            final String method = parts.length > 0 ? parts[0] : "";
            final String path = parts.length > 1 ? parts[1] : "/";

            if (!"GET".equals(method)) {
                Http.writeResponse(out, 405, "Method Not Allowed", HTML,
                        "<h1>405 Method Not Allowed</h1>");
                return;
            }

            route(path, out);
        } catch (final IOException e) {
            // client gone mid-request — nothing to do
        }
    }

    private void route(final String path, final OutputStream out) throws IOException {
        switch (path) {
            case "/events":
                Http.writeResponse(out, 200, "OK", HTML, store.eventsHtml());
                break;
            case "/active":
                Http.writeResponse(out, 200, "OK", HTML, store.activeUsersHtml());
                break;
            case "/":
            case "/index.html":
                serveStatic("index.html", out);
                break;
            default:
                serveStatic(path.startsWith("/") ? path.substring(1) : path, out);
        }
    }

    private void serveStatic(final String name, final OutputStream out) throws IOException {
        // prevent path traversal; only serve flat files from /www
        if (name.isEmpty() || name.contains("..") || name.contains("/")) {
            Http.writeResponse(out, 404, "Not Found", HTML, "<h1>404 Not Found</h1>");
            return;
        }
        try (InputStream res = getClass().getResourceAsStream("/www/" + name)) {
            if (res == null) {
                Http.writeResponse(out, 404, "Not Found", HTML, "<h1>404 Not Found</h1>");
                return;
            }
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
            res.transferTo(body);
            Http.writeResponse(out, 200, "OK", contentType(name), body.toByteArray());
        }
    }

    private static String contentType(final String name) {
        if (name.endsWith(".html")) {
            return HTML;
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private static void drainHeaders(final InputStream in) throws IOException {
        String line = Http.readLineCRLF(in);
        while (line != null && !line.isEmpty()) {
            line = Http.readLineCRLF(in);
        }
    }
}
