package eapli.aisafe.app.weather.console.remote.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;
import eapli.aisafe.app.weather.console.remote.protocol.WeatherProtocol;

public class WeatherRemoteServer implements Runnable {

    private final int port;
    private final RemoteAccessLogger logger;
    private volatile ServerSocket serverSocket;

    public WeatherRemoteServer() {
        this(WeatherProtocol.PORT);
    }

    WeatherRemoteServer(final int port) {
        this.port = port;
        this.logger = RemoteAccessLogger.fromSystemProperties();
    }

    @Override
    public void run() {
        try (ServerSocket ss = createServerSocket(port)) {
            this.serverSocket = ss;
            System.out.println("[WeatherRemoteServer] Listening on port " + port);
            while (!ss.isClosed()) {
                try {
                    final Socket client = ss.accept();
                    final Thread t = new Thread(new WeatherClientHandler(client, logger));
                    t.setDaemon(true);
                    t.start();
                } catch (final IOException e) {
                    if (!ss.isClosed()) {
                        System.err.println("[WeatherRemoteServer] accept error: " + e.getMessage());
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("[WeatherRemoteServer] could not start on port "
                    + port + ": " + e.getMessage());
        }
    }

    ServerSocket createServerSocket(final int p) throws IOException {
        return new ServerSocket(p);
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (final IOException e) {
            System.err.println("[WeatherRemoteServer] error closing: " + e.getMessage());
        }
    }
}
