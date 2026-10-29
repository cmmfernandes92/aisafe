package eapli.aisafe.app.atcc.console.infrastructure.tcp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Test;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;

public class AtccRemoteClientTest {

    // ------------------------------------------------------------------ helpers

    private static AtccRemoteClient clientWith(final String stdin, final PrintStream out) {
        return new AtccRemoteClient(new Scanner(stdin), out);
    }

    private static byte[] responseOkOnly() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        new DataOutputStream(raw).writeByte(AtccProtocol.RESPONSE_OK);
        return raw.toByteArray();
    }

    private static byte[] responseWith(final byte code, final String msg) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(code);
        AtccRemoteClient.writeString(out, msg);
        return raw.toByteArray();
    }

    private static byte[] listFleetResponse(final int count) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(AtccProtocol.RESPONSE_DATA);
        out.writeShort(count);
        return raw.toByteArray();
    }

    private static byte[] listPilotsResponse(final int count) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(AtccProtocol.RESPONSE_DATA);
        out.writeShort(count);
        return raw.toByteArray();
    }

    private static DataInputStream wrap(final byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static DataOutputStream sink(final ByteArrayOutputStream raw) {
        return new DataOutputStream(raw);
    }

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    // ------------------------------------------------------------------ sendLogin

    @Test
    public void ensureSendLoginReturnsTrueOnResponseOk() throws IOException {
        final boolean ok = clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogin(sink(new ByteArrayOutputStream()), wrap(responseOkOnly()), "u", "p");
        assertTrue(ok);
    }

    @Test
    public void ensureSendLoginReturnsFalseOnResponseError() throws IOException {
        final boolean ok = clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogin(sink(new ByteArrayOutputStream()),
                        wrap(responseWith(AtccProtocol.RESPONSE_ERROR, "bad")), "u", "p");
        assertFalse(ok);
    }

    @Test
    public void ensureSendLoginReturnsFalseOnUnexpectedResponse() throws IOException {
        final boolean ok = clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogin(sink(new ByteArrayOutputStream()),
                        wrap(responseWith((byte) 0x7F, "x")), "u", "p");
        assertFalse(ok);
    }

    @Test
    public void ensureSendLoginWritesLoginByteAndCredentials() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogin(sink(sent), wrap(responseOkOnly()), "user", "pw");

        assertEquals(AtccProtocol.LOGIN, new DataInputStream(
                new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ sendLogout

    @Test
    public void ensureSendLogoutWritesLogoutByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogout(sink(sent), wrap(responseOkOnly()));
        assertEquals(AtccProtocol.LOGOUT, sent.toByteArray()[0]);
    }

    // ------------------------------------------------------------------ menu option 0

    @Test
    public void ensureMenuOption0LogsOutAndReturns() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent), wrap(responseOkOnly()));
        assertEquals(AtccProtocol.LOGOUT, sent.toByteArray()[0]);
    }

    @Test
    public void ensureMenuUnknownOptionLoopsUntilLogout() throws IOException {
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("9\n0\n", new PrintStream(display))
                .runMenu(sink(new ByteArrayOutputStream()), wrap(responseOkOnly()));
        assertTrue(display.toString(StandardCharsets.UTF_8).contains("Unknown option"));
    }

    // ------------------------------------------------------------------ menu option 1 — add aircraft

    @Test
    public void ensureMenuOption1SendsAddAircraftByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final String stdin = "1\nCS-TFM\n1\n150\n20\n8\nPT\n0\n";
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(AtccProtocol.RESPONSE_ERROR, "stub"), responseOkOnly())));
        assertEquals(AtccProtocol.ADD_AIRCRAFT,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ menu option 2 — decommission

    @Test
    public void ensureMenuOption2SendsDecommissionByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("2\nCS-TFM\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseOkOnly(), responseOkOnly())));
        assertEquals(AtccProtocol.DECOMMISSION_AIRCRAFT,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ menu option 3 — list fleet

    @Test
    public void ensureMenuOption3SendsListFleetByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("3\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(listFleetResponse(0), responseOkOnly())));
        assertEquals(AtccProtocol.LIST_FLEET,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    @Test
    public void ensureMenuOption3PrintsFailOnNonDataResponse() throws IOException {
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("3\n0\n", new PrintStream(display))
                .runMenu(sink(new ByteArrayOutputStream()),
                        wrap(concat(responseWith(AtccProtocol.RESPONSE_ERROR, "boom"), responseOkOnly())));
        assertTrue(display.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    // ------------------------------------------------------------------ menu option 4 — create route

    @Test
    public void ensureMenuOption4SendsCreateFlightRouteByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("4\nTP123\nLIS\nOPO\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(AtccProtocol.RESPONSE_DATA, "TP123"), responseOkOnly())));
        assertEquals(AtccProtocol.CREATE_FLIGHT_ROUTE,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ menu option 5 — deactivate route

    @Test
    public void ensureMenuOption5SendsDeactivateFlightRouteByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("5\nTP123\n2026-07-01\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseOkOnly(), responseOkOnly())));
        assertEquals(AtccProtocol.DEACTIVATE_FLIGHT_ROUTE,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ menu option 6 — add pilot

    @Test
    public void ensureMenuOption6SendsAddPilotByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("6\npilot@tap.pt\n1\n1\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(AtccProtocol.RESPONSE_ERROR, "stub"), responseOkOnly())));
        assertEquals(AtccProtocol.ADD_PILOT,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ menu option 7 — list pilots

    @Test
    public void ensureMenuOption7SendsListPilotsByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("7\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(listPilotsResponse(0), responseOkOnly())));
        assertEquals(AtccProtocol.LIST_PILOTS,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    @Test
    public void ensureMenuOption7PrintsFailOnNonDataResponse() throws IOException {
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("7\n0\n", new PrintStream(display))
                .runMenu(sink(new ByteArrayOutputStream()),
                        wrap(concat(responseWith(AtccProtocol.RESPONSE_ERROR, "boom"), responseOkOnly())));
        assertTrue(display.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    // ------------------------------------------------------------------ menu option 8 — deactivate pilot

    @Test
    public void ensureMenuOption8SendsDeactivatePilotByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("8\npilot@tap.pt\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseOkOnly(), responseOkOnly())));
        assertEquals(AtccProtocol.DEACTIVATE_PILOT,
                new DataInputStream(new ByteArrayInputStream(sent.toByteArray())).readByte());
    }

    // ------------------------------------------------------------------ printResponse paths

    @Test
    public void ensurePrintResponsePrintsOk() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .printResponse(wrap(new byte[]{AtccProtocol.RESPONSE_OK}));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("[ OK ]"));
    }

    @Test
    public void ensurePrintResponsePrintsOkWithData() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .printResponse(wrap(responseWith(AtccProtocol.RESPONSE_DATA, "payload")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("payload"));
    }

    @Test
    public void ensurePrintResponsePrintsFailWithMessage() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .printResponse(wrap(responseWith(AtccProtocol.RESPONSE_ERROR, "no")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    // ------------------------------------------------------------------ printMenu

    @Test
    public void ensurePrintMenuRendersAllOptions() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out)).printMenu();
        final String rendered = out.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("Add aircraft"));
        assertTrue(rendered.contains("List fleet"));
        assertTrue(rendered.contains("Logout"));
    }

    // ------------------------------------------------------------------ readFully + readString

    @Test
    public void ensureReadFullyThrowsEofWhenStreamIsClosed() {
        try {
            AtccRemoteClient.readFully(wrap(new byte[0]), new byte[4], 0, 4);
            fail("Expected EOFException");
        } catch (final IOException expected) {
            assertTrue(expected instanceof EOFException);
        }
    }

    @Test
    public void ensureReadStringRoundTrip() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        AtccRemoteClient.writeString(sink(raw), "hello");
        assertEquals("hello", AtccRemoteClient.readString(wrap(raw.toByteArray())));
    }

    // ------------------------------------------------------------------ run + connect + main

    @Test
    public void ensureRunPerformsLoginThenLogout() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("atcc@test.com\nsecret\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .run(wrap(concat(responseOkOnly(), responseOkOnly())), sink(sent));
        assertEquals(AtccProtocol.LOGIN, sent.toByteArray()[0]);
    }

    @Test
    public void ensureRunPrintsFailWhenLoginRejected() throws IOException {
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("atcc@test.com\nwrong\n", new PrintStream(display))
                .run(wrap(responseWith(AtccProtocol.RESPONSE_ERROR, "bad")),
                        sink(new ByteArrayOutputStream()));
        assertTrue(display.toString(StandardCharsets.UTF_8).contains("Login failed"));
    }

    @Test
    public void ensureConnectAgainstLiveServerWorksThroughLogout() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();
            final Thread serverThread = new Thread(() -> {
                try (Socket s = server.accept();
                     DataInputStream in = new DataInputStream(s.getInputStream());
                     DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
                    in.readByte(); // LOGIN
                    AtccRemoteClient.readString(in); // user
                    AtccRemoteClient.readString(in); // pw
                    out.writeByte(AtccProtocol.RESPONSE_OK);
                    out.flush();
                    in.readByte(); // LOGOUT
                    out.writeByte(AtccProtocol.RESPONSE_OK);
                    out.flush();
                } catch (final IOException ignored) { }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            clientWith("atcc@test.com\nsecret\n0\n",
                    new PrintStream(new ByteArrayOutputStream()))
                    .connect("localhost", port);
            serverThread.join(1000);
        }
    }

    @Test
    public void ensureConnectPrintsFailWhenServerUnavailable() throws Exception {
        final int unusedPort;
        try (ServerSocket s = new ServerSocket(0)) { unusedPort = s.getLocalPort(); }
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("", new PrintStream(display)).connect("localhost", unusedPort);
        assertTrue(display.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    @Test
    public void ensureMainParsesArgsAndDelegatesToConnect() throws Exception {
        final int unusedPort;
        try (ServerSocket s = new ServerSocket(0)) { unusedPort = s.getLocalPort(); }
        final java.io.InputStream prevIn = System.in;
        final PrintStream prevOut = System.out;
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream(new byte[0]));
        System.setOut(new PrintStream(display));
        try {
            AtccRemoteClient.main(new String[]{"localhost", String.valueOf(unusedPort)});
            AtccRemoteClient.main(new String[]{});
        } finally {
            System.setIn(prevIn);
            System.setOut(prevOut);
        }
    }

    @Test
    public void ensureDefaultConstructorWires() {
        assertNotNull(new AtccRemoteClient());
    }
}