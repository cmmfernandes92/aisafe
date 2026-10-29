package eapli.aisafe.app.logserver;

import eapli.aisafe.app.logserver.http.LogHttpServer;
import eapli.aisafe.app.logserver.store.LogStore;
import eapli.aisafe.app.logserver.udp.RemoteAccessLogReceiver;

/**
 * Remote Accesses Logging Server (RCOMP US090 + US091).
 *
 * <p>Runs two servers over one shared {@link LogStore}:</p>
 * <ul>
 *   <li><b>US090</b> — a UDP receiver collecting remote-access event datagrams;</li>
 *   <li><b>US091</b> — an HTTP server serving two AJAX status pages.</li>
 * </ul>
 *
 * <p>Standalone and non-interactive: meant to run headless on its own cloud node.
 * Usage: {@code java -jar aisafe.app.logserver.jar [udpPort] [httpPort]}
 * (defaults {@code 9090} / {@code 8090}).</p>
 */
public final class LogServerApp {

    private static final int DEFAULT_UDP_PORT  = 9090;
    private static final int DEFAULT_HTTP_PORT = 8090;

    private LogServerApp() {
    }

    public static void main(final String[] args) {
        final int udpPort  = argOrDefault(args, 0, DEFAULT_UDP_PORT);
        final int httpPort = argOrDefault(args, 1, DEFAULT_HTTP_PORT);

        final LogStore store = new LogStore();

        // US090 — UDP receiver on its own thread.
        final Thread udp = new Thread(new RemoteAccessLogReceiver(udpPort, store), "udp-receiver");
        udp.setDaemon(true);
        udp.start();

        System.out.println("[LogServer] started. UDP=" + udpPort + " HTTP=" + httpPort);
        System.out.println("[LogServer] open http://<this-host>:" + httpPort + "/");

        // US091 — HTTP server on the main thread (keeps the process alive).
        new LogHttpServer(httpPort, store).run();
    }

    private static int argOrDefault(final String[] args, final int index, final int fallback) {
        if (args.length > index) {
            try {
                return Integer.parseInt(args[index]);
            } catch (final NumberFormatException ignored) {
                // fall through to default
            }
        }
        return fallback;
    }
}
