package eapli.aisafe.app.common.console.remote.logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * US090 — emits remote-access events to the Remote Accesses Logging Server as
 * fire-and-forget UDP datagrams.
 *
 * <p>UDP gives no delivery feedback: the emitter never waits for a reply, so it
 * can neither block nor slow down a login, and an occasionally lost log line
 * leaves the service unaffected. Every failure is swallowed — logging must never
 * break the service it observes.</p>
 *
 * <p>One datagram per event, a single text line:</p>
 * <pre>eventType|isoTimestamp|username|clientIp|clientPort|service</pre>
 *
 * <p>The target log-server address is read from system properties
 * ({@code aisafe.logserver.host}, default {@code localhost};
 * {@code aisafe.logserver.port}, default {@code 9090}) so the Logging Server can
 * run on a separate node. A {@link #disabled()} instance is a no-op, used in
 * tests and wherever logging is not wired.</p>
 */
public final class RemoteAccessLogger {

    public static final String HOST_PROPERTY = "aisafe.logserver.host";
    public static final String PORT_PROPERTY = "aisafe.logserver.port";
    public static final String DEFAULT_HOST  = "localhost";
    public static final int    DEFAULT_PORT  = 9090;

    /** {@code null} socket => disabled (no-op). */
    private final DatagramSocket socket;
    private final InetAddress host;
    private final int port;

    private RemoteAccessLogger() {
        this.socket = null;
        this.host = null;
        this.port = 0;
    }

    private RemoteAccessLogger(final DatagramSocket socket, final InetAddress host, final int port) {
        this.socket = socket;
        this.host = host;
        this.port = port;
    }

    /** A no-op logger; never opens a socket and never sends. */
    public static RemoteAccessLogger disabled() {
        return new RemoteAccessLogger();
    }

    /** Builds a logger from the {@code aisafe.logserver.*} system properties. */
    public static RemoteAccessLogger fromSystemProperties() {
        final String h = System.getProperty(HOST_PROPERTY, DEFAULT_HOST);
        int p = DEFAULT_PORT;
        try {
            p = Integer.parseInt(System.getProperty(PORT_PROPERTY, Integer.toString(DEFAULT_PORT)));
        } catch (final NumberFormatException ignored) {
            // keep default port
        }
        return create(h, p);
    }

    /** Builds a logger targeting {@code host:port}; returns {@link #disabled()} if the socket can't open. */
    public static RemoteAccessLogger create(final String host, final int port) {
        try {
            return new RemoteAccessLogger(new DatagramSocket(), InetAddress.getByName(host), port);
        } catch (final IOException e) {
            return disabled();
        }
    }

    /**
     * Sends one event datagram. No-op when disabled or on any I/O failure.
     *
     * @param event    the event type
     * @param username the (attempted) username, may be {@code null}
     * @param client   the accepted client socket, may be {@code null}
     * @param service  the service tag (e.g. {@code US44}, {@code US78}, {@code US86})
     */
    public void log(final RemoteAccessEvent event, final String username,
            final Socket client, final String service) {
        if (socket == null) {
            return;
        }
        final String ip = (client != null && client.getInetAddress() != null)
                ? client.getInetAddress().getHostAddress() : "-";
        final int clientPort = client != null ? client.getPort() : 0;

        final String line = String.join("|",
                event.name(),
                LocalDateTime.now().toString(),
                username == null ? "-" : username,
                ip,
                Integer.toString(clientPort),
                service);

        final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        try {
            socket.send(new DatagramPacket(bytes, bytes.length, host, port));
        } catch (final IOException ignored) {
            // logging must never break the service
        }
    }
}
