package eapli.aisafe.app.logserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP wire helpers: CRLF-framed line reading and response writing.
 *
 * <p>HTTP frames its header section with an end-of-data marker — each line ends
 * in CR+LF and a blank line ends the headers. We read byte-by-byte (the line
 * length is not known in advance) and strip the CR.</p>
 */
public final class Http {

    private static final int CR = 13;
    private static final int LF = 10;

    private Http() {
    }

    /** Reads one CRLF-terminated line; returns {@code null} at end of stream. */
    public static String readLineCRLF(final InputStream in) throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b = in.read();
        if (b == -1) {
            return null;
        }
        while (b != -1 && b != LF) {
            if (b != CR) {
                buf.write(b);
            }
            b = in.read();
        }
        return buf.toString(StandardCharsets.US_ASCII);
    }

    /** Writes a full HTTP/1.0 response with {@code Connection: close} and the given body. */
    public static void writeResponse(final OutputStream out, final int status, final String reason,
            final String contentType, final byte[] body) throws IOException {
        final StringBuilder head = new StringBuilder();
        head.append("HTTP/1.0 ").append(status).append(' ').append(reason).append("\r\n");
        head.append("Content-Type: ").append(contentType).append("\r\n");
        head.append("Content-Length: ").append(body.length).append("\r\n");
        head.append("Connection: close\r\n");
        head.append("\r\n");
        out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    /** Convenience overload for text bodies. */
    public static void writeResponse(final OutputStream out, final int status, final String reason,
            final String contentType, final String body) throws IOException {
        writeResponse(out, status, reason, contentType, body.getBytes(StandardCharsets.UTF_8));
    }
}
