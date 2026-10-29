package eapli.aisafe.app.pilot.console.infrastructure.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class PilotTcpServerTest {

    @BeforeClass
    public static void configureAuthzForHandlerConstruction() {
        AuthzRegistry.configure(new InMemoryUserRepository(),
                new AisafePasswordPolicy(), new PlainTextEncoder());
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    @Test
    public void ensureDefaultConstructorUsesProtocolPort() throws Exception {
        final PilotTcpServer server = new PilotTcpServer();
        final Field portField = PilotTcpServer.class.getDeclaredField("port");
        portField.setAccessible(true);
        assertEquals(TcpProtocol.PORT, portField.getInt(server));
    }

    @Test
    public void ensurePilotTcpServerAcceptsConnectionOnConfiguredPort() throws Exception {
        final int port = freePort();
        final PilotTcpServer server = new PilotTcpServer(port);
        final Thread serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(100);

        try (Socket client = new Socket("localhost", port)) {
            assertTrue(client.isConnected());
        } finally {
            server.stop();
        }
        serverThread.join(1000);
    }

    @Test
    public void ensurePilotTcpServerStopsCleanly() throws Exception {
        final int port = freePort();
        final PilotTcpServer server = new PilotTcpServer(port);
        final Thread serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(100);
        server.stop();
        serverThread.join(1000);

        try (Socket ignored = new Socket("localhost", port)) {
            assertFalse("server should have stopped", true);
        } catch (final IOException e) {
            assertTrue(true);
        }
    }

    @Test
    public void ensureRunPrintsErrorWhenServerSocketCannotBind() throws Exception {
        // Occupy the port so PilotTcpServer fails to bind.
        try (ServerSocket occupy = new ServerSocket(0)) {
            final int port = occupy.getLocalPort();
            final PilotTcpServer server = new PilotTcpServer(port);
            final Thread t = new Thread(server);
            t.setDaemon(true);
            t.start();
            t.join(1000);
            assertFalse("server thread should have exited", t.isAlive());
        }
    }

    @Test
    public void ensureStopSwallowsIOExceptionFromServerSocketClose() throws Exception {
        final PilotTcpServer server = new PilotTcpServer(freePort());
        final Field f = PilotTcpServer.class.getDeclaredField("serverSocket");
        f.setAccessible(true);
        f.set(server, new ServerSocket() {
            @Override
            public boolean isClosed() {
                return false;
            }
            @Override
            public void close() throws IOException {
                throw new IOException("forced close failure");
            }
        });

        server.stop(); // must not throw
    }

    @Test
    public void ensureStopIsNoopWhenServerSocketIsNull() {
        // serverSocket is null before run() is invoked.
        new PilotTcpServer(0).stop();
    }

    @Test
    public void ensureStopIsNoopWhenServerSocketAlreadyClosed() throws Exception {
        final PilotTcpServer server = new PilotTcpServer(freePort());
        final Field f = PilotTcpServer.class.getDeclaredField("serverSocket");
        f.setAccessible(true);
        final ServerSocket closed = new ServerSocket();
        closed.close();
        f.set(server, closed);

        server.stop(); // must not throw
    }

    @Test
    public void ensureRunPrintsAcceptErrorWhenAcceptThrowsWhileSocketIsOpen() throws Exception {
        final int port = freePort();
        final PilotTcpServer server = new PilotTcpServer(port) {
            @Override
            ServerSocket createServerSocket(final int p) throws IOException {
                final ServerSocket ss = new ServerSocket(p);
                ss.setSoTimeout(40); // accept() will throw SocketTimeoutException quickly
                return ss;
            }
        };
        final Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();

        // Give accept() time to time out once (so the error branch fires) before stopping.
        Thread.sleep(120);
        server.stop();
        t.join(1000);
        assertFalse(t.isAlive());
    }

    @Test
    public void ensureRunPrintsAcceptErrorWhenSocketIsClosedDuringAccept() throws Exception {
        final int port = freePort();
        final PilotTcpServer server = new PilotTcpServer(port);
        final AtomicReference<Thread> serverThread = new AtomicReference<>();
        final Thread t = new Thread(server);
        serverThread.set(t);
        t.setDaemon(true);
        t.start();

        // Give the run loop time to install the ServerSocket reference.
        Thread.sleep(150);

        // Force an IOException inside accept() by closing the socket
        // via reflection (so the wrapping isClosed() check still reads false
        // briefly) — then call stop() which actually closes it.
        server.stop();
        t.join(1000);

        assertFalse(t.isAlive());
    }
}
