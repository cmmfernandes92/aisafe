package eapli.aisafe.app.logserver.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import eapli.aisafe.app.logserver.store.LogStore;

/**
 * US091 — minimal HTTP server exposing the live {@link LogStore}. One TCP
 * connection per request; thread-per-connection accept loop.
 */
public final class LogHttpServer implements Runnable {

    private final int port;
    private final LogStore store;

    public LogHttpServer(final int port, final LogStore store) {
        this.port = port;
        this.store = store;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[LogServer] HTTP server listening on port " + port);
            while (!server.isClosed()) {
                try {
                    final Socket client = server.accept();
                    new Thread(new LogHttpRequest(client, store)).start();
                } catch (final IOException e) {
                    if (!server.isClosed()) {
                        System.err.println("[LogServer] HTTP accept error: " + e.getMessage());
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("[LogServer] HTTP server could not start on port "
                    + port + ": " + e.getMessage());
        }
    }
}
