package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;

public class AtccTcpServer implements Runnable {

    private final int port;
    private final RemoteAccessLogger logger;
    private volatile ServerSocket serverSocket;

    public AtccTcpServer() {
        this(AtccProtocol.PORT);
    }

    AtccTcpServer(final int port) {
        this.port = port;
        this.logger = RemoteAccessLogger.fromSystemProperties();
    }

    @Override
    public void run() {
        try (ServerSocket ss = createServerSocket(port)) {
            this.serverSocket = ss;
            System.out.println("[AtccTcpServer] listening on port " + port);
            while (!ss.isClosed()) {
                try {
                    final Socket client = ss.accept();
                    new Thread(new AtccConnectionHandler(client, logger)).start();
                } catch (final IOException e) {
                    if (!ss.isClosed()) {
                        System.err.println("[AtccTcpServer] accept error: " + e.getMessage());
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("[AtccTcpServer] could not start on port "
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
            System.err.println("[AtccTcpServer] error closing: " + e.getMessage());
        }
    }
}
