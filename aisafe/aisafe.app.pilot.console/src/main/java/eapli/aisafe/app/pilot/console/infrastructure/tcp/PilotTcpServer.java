package eapli.aisafe.app.pilot.console.infrastructure.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;
import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;

public class PilotTcpServer implements Runnable {

    private final int port;
    private final RemoteAccessLogger logger;
    private volatile ServerSocket serverSocket;

    public PilotTcpServer() {
        this(TcpProtocol.PORT);
    }

    PilotTcpServer(final int port) {
        this.port = port;
        this.logger = RemoteAccessLogger.fromSystemProperties();
    }

    @Override
    public void run() {
        try (ServerSocket ss = createServerSocket(port)) {
            this.serverSocket = ss;
            while (!ss.isClosed()) {
                try {
                    final Socket client = ss.accept();
                    new Thread(new PilotConnectionHandler(client, logger)).start();
                } catch (final IOException e) {
                    if (!ss.isClosed()) {
                        System.err.println("[PilotTcpServer] accept error: " + e.getMessage());
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("[PilotTcpServer] could not start on port " + port + ": " + e.getMessage());
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
            System.err.println("[PilotTcpServer] error closing: " + e.getMessage());
        }
    }
}