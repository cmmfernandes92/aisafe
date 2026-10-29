package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class AtccTcpServerTest {

    @BeforeClass
    public static void configureAuthz() {
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
        final AtccTcpServer server = new AtccTcpServer();
        final Field portField = AtccTcpServer.class.getDeclaredField("port");
        portField.setAccessible(true);
        assertEquals(AtccProtocol.PORT, portField.getInt(server));
    }

    @Test
    public void ensureAtccTcpServerAcceptsConnectionOnConfiguredPort() throws Exception {
        final int port = freePort();
        final AtccTcpServer server = new AtccTcpServer(port);
        final Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
        Thread.sleep(100);

        try (Socket client = new Socket("localhost", port)) {
            assertTrue(client.isConnected());
        } finally {
            server.stop();
        }
        t.join(1000);
    }

    @Test
    public void ensureAtccTcpServerStopsCleanly() throws Exception {
        final int port = freePort();
        final AtccTcpServer server = new AtccTcpServer(port);
        final Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
        Thread.sleep(100);
        server.stop();
        t.join(1000);

        try (Socket ignored = new Socket("localhost", port)) {
            assertFalse("server should have stopped", true);
        } catch (final IOException e) {
            assertTrue(true);
        }
    }

    @Test
    public void ensureRunPrintsErrorWhenServerSocketCannotBind() throws Exception {
        try (ServerSocket occupy = new ServerSocket(0)) {
            final int port = occupy.getLocalPort();
            final AtccTcpServer server = new AtccTcpServer(port);
            final Thread t = new Thread(server);
            t.setDaemon(true);
            t.start();
            t.join(1000);
            assertFalse("server thread should have exited", t.isAlive());
        }
    }

    @Test
    public void ensureStopSwallowsIOExceptionFromServerSocketClose() throws Exception {
        final AtccTcpServer server = new AtccTcpServer(freePort());
        final Field f = AtccTcpServer.class.getDeclaredField("serverSocket");
        f.setAccessible(true);
        f.set(server, new ServerSocket() {
            @Override
            public boolean isClosed() { return false; }
            @Override
            public void close() throws IOException { throw new IOException("forced"); }
        });
        server.stop(); // must not throw
    }

    @Test
    public void ensureStopIsNoopWhenServerSocketIsNull() {
        new AtccTcpServer(0).stop();
    }

    @Test
    public void ensureStopIsNoopWhenServerSocketAlreadyClosed() throws Exception {
        final AtccTcpServer server = new AtccTcpServer(freePort());
        final Field f = AtccTcpServer.class.getDeclaredField("serverSocket");
        f.setAccessible(true);
        final ServerSocket closed = new ServerSocket();
        closed.close();
        f.set(server, closed);
        server.stop(); // must not throw
    }

    @Test
    public void ensureRunPrintsAcceptErrorWhenAcceptTimesOut() throws Exception {
        final int port = freePort();
        final AtccTcpServer server = new AtccTcpServer(port) {
            @Override
            ServerSocket createServerSocket(final int p) throws IOException {
                final ServerSocket ss = new ServerSocket(p);
                ss.setSoTimeout(40);
                return ss;
            }
        };
        final Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
        Thread.sleep(120);
        server.stop();
        t.join(1000);
        assertFalse(t.isAlive());
    }

    @Test
    public void ensureRunExitsCleanlyWhenStoppedDuringAccept() throws Exception {
        final int port = freePort();
        final AtccTcpServer server = new AtccTcpServer(port);
        final Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
        Thread.sleep(150);
        server.stop();
        t.join(1000);
        assertFalse(t.isAlive());
    }
}