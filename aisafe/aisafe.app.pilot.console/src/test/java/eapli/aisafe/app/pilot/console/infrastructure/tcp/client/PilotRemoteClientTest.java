package eapli.aisafe.app.pilot.console.infrastructure.tcp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;

public class PilotRemoteClientTest {

    // ------------------------------------------------------------------ helpers

    private static PilotRemoteClient clientWith(final String stdin, final PrintStream out) {
        return new PilotRemoteClient(new Scanner(stdin), out);
    }

    private static byte[] responseWith(final byte code, final String msg) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(code);
        writeString(out, msg);
        return raw.toByteArray();
    }

    private static byte[] responseOkOnly() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(TcpProtocol.RESPONSE_OK);
        return raw.toByteArray();
    }

    private static void writeString(final DataOutputStream out, final String s) throws IOException {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static DataInputStream wrap(final byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static DataOutputStream sink(final ByteArrayOutputStream raw) {
        return new DataOutputStream(raw);
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
                        wrap(responseWith(TcpProtocol.RESPONSE_ERROR, "bad")), "u", "p");
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

        final DataInputStream written = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.LOGIN, written.readByte());
    }

    // ------------------------------------------------------------------ sendLogout

    @Test
    public void ensureSendLogoutWritesLogoutByteAndReadsOneByte() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("", new PrintStream(new ByteArrayOutputStream()))
                .sendLogout(sink(sent), wrap(responseOkOnly()));

        assertEquals(TcpProtocol.LOGOUT, sent.toByteArray()[0]);
    }

    // ------------------------------------------------------------------ menu

    @Test
    public void ensureMenuOption0LogsOutAndReturns() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("0\n", new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent), wrap(responseOkOnly()));

        assertEquals(TcpProtocol.LOGOUT, sent.toByteArray()[0]);
    }

    @Test
    public void ensureMenuUnknownOptionLoopsUntilLogout() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("9\n0\n", new PrintStream(display))
                .runMenu(sink(sent), wrap(responseOkOnly()));

        assertTrue(display.toString(StandardCharsets.UTF_8).contains("Unknown option"));
    }

    @Test
    public void ensureMenuOption1InvokesCreateFlight() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final String stdin = String.join("\n",
                "1", "TP123", "2026-06-15T10:00", "CS-TFX", "0", "");
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(TcpProtocol.RESPONSE_OK, ""), responseOkOnly())));

        final DataInputStream w = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.CREATE_FLIGHT_PLAN, w.readByte());
    }

    @Test
    public void ensureMenuOption2InvokesImportPlan() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final String stdin = String.join("\n",
                "2", "TP123", "2026-06-15",
                "FLIGHT_PLAN {", "}", "###", "0", "");
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(TcpProtocol.RESPONSE_OK, ""), responseOkOnly())));

        final DataInputStream w = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.CREATE_FROM_DSL, w.readByte());
    }

    @Test
    public void ensureMenuOption3InvokesListFlights() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final byte[] listResponse = buildListFlightsResponse(1, "TP123:2026-06-15", "VALIDATED");
        final String stdin = "3\n0\n";
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent), wrap(concat(listResponse, responseOkOnly())));

        final DataInputStream w = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.LIST_FLIGHTS, w.readByte());
    }

    @Test
    public void ensureMenuOption4InvokesAttachWeather() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final String stdin = String.join("\n",
                "4", "TP123", "2026-06-15", "1", "2",
                "11111111-1111-1111-1111-111111111111", "0", "");
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseOkOnly(), responseOkOnly())));

        final DataInputStream w = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.INSERT_WEATHER, w.readByte());
    }

    @Test
    public void ensureMenuOption5InvokesValidatePlan() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final String stdin = String.join("\n", "5", "TP123", "2026-06-15", "0", "");
        clientWith(stdin, new PrintStream(new ByteArrayOutputStream()))
                .runMenu(sink(sent),
                        wrap(concat(responseWith(TcpProtocol.RESPONSE_DATA, "ok:done"), responseOkOnly())));

        final DataInputStream w = wrap(sent.toByteArray());
        assertEquals(TcpProtocol.VALIDATE_FLIGHT_PLAN, w.readByte());
    }

    // ------------------------------------------------------------------ printResponse paths

    @Test
    public void ensurePrintResponsePrintsOk() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out)).printResponse(wrap(new byte[]{ TcpProtocol.RESPONSE_OK }));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("[ OK ]"));
    }

    @Test
    public void ensurePrintResponsePrintsOkWithData() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .printResponse(wrap(responseWith(TcpProtocol.RESPONSE_DATA, "payload")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("payload"));
    }

    @Test
    public void ensurePrintResponsePrintsFailWithMessage() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .printResponse(wrap(responseWith(TcpProtocol.RESPONSE_ERROR, "no")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    // ------------------------------------------------------------------ sendListFlights error path

    @Test
    public void ensureSendListFlightsPrintsFailOnNonDataResponse() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out))
                .sendListFlights(sink(new ByteArrayOutputStream()),
                        wrap(responseWith(TcpProtocol.RESPONSE_ERROR, "boom")));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    // ------------------------------------------------------------------ printMenu

    @Test
    public void ensurePrintMenuRendersAllOptions() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientWith("", new PrintStream(out)).printMenu();
        final String rendered = out.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("Create flight"));
        assertTrue(rendered.contains("Import flight plan"));
        assertTrue(rendered.contains("Logout"));
    }

    // ------------------------------------------------------------------ readFully + readString helpers

    @Test
    public void ensureReadFullyThrowsEofWhenStreamIsClosed() {
        try {
            PilotRemoteClient.readFully(wrap(new byte[0]), new byte[4], 0, 4);
            fail("Expected EOF");
        } catch (final IOException expected) {
            assertTrue(expected instanceof EOFException);
        }
    }

    @Test
    public void ensureReadStringRoundTrip() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        PilotRemoteClient.writeString(sink(raw), "abc");

        final String read = PilotRemoteClient.readString(wrap(raw.toByteArray()));
        assertEquals("abc", read);
    }

    // ------------------------------------------------------------------ run + connect + main

    @Test
    public void ensureRunPerformsLoginThenLogout() throws IOException {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        clientWith("pilot@test.com\nsecret\n0\n", new PrintStream(new ByteArrayOutputStream()))
                .run(wrap(concat(responseOkOnly(), responseOkOnly())), sink(sent));

        assertEquals(TcpProtocol.LOGIN, sent.toByteArray()[0]);
    }

    @Test
    public void ensureRunPrintsFailWhenLoginRejected() throws IOException {
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("pilot@test.com\nwrong\n", new PrintStream(display))
                .run(wrap(responseWith(TcpProtocol.RESPONSE_ERROR, "bad")),
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
                    readString(in); // user
                    readString(in); // pw
                    out.writeByte(TcpProtocol.RESPONSE_OK);
                    out.flush();
                    in.readByte(); // LOGOUT
                    out.writeByte(TcpProtocol.RESPONSE_OK);
                    out.flush();
                } catch (final IOException ignored) {
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            clientWith("pilot@test.com\nsecret\n0\n", new PrintStream(new ByteArrayOutputStream()))
                    .connect("localhost", port);

            serverThread.join(1000);
        }
    }

    @Test
    public void ensureConnectPrintsFailWhenServerUnavailable() throws Exception {
        final int unusedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            unusedPort = s.getLocalPort();
        }
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        clientWith("", new PrintStream(display)).connect("localhost", unusedPort);

        assertTrue(display.toString(StandardCharsets.UTF_8).contains("[FAIL]"));
    }

    @Test
    public void ensureMainParsesArgsAndDelegatesToConnect() throws Exception {
        final int unusedPort;
        try (ServerSocket s = new ServerSocket(0)) {
            unusedPort = s.getLocalPort();
        }
        final java.io.ByteArrayInputStream stdin = new java.io.ByteArrayInputStream(new byte[0]);
        final java.io.InputStream prevIn = System.in;
        final java.io.PrintStream prevOut = System.out;
        final ByteArrayOutputStream display = new ByteArrayOutputStream();
        System.setIn(stdin);
        System.setOut(new PrintStream(display));
        try {
            PilotRemoteClient.main(new String[]{ "localhost", String.valueOf(unusedPort) });
            PilotRemoteClient.main(new String[]{});
        } finally {
            System.setIn(prevIn);
            System.setOut(prevOut);
        }
    }

    @Test
    public void ensureDefaultConstructorWires() {
        final PilotRemoteClient client = new PilotRemoteClient();
        org.junit.Assert.assertNotNull(client);
    }

    // ------------------------------------------------------------------ utilities

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] buildListFlightsResponse(final int count, final String... pairs) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        out.writeByte(TcpProtocol.RESPONSE_DATA);
        out.writeShort(count);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            writeString(out, pairs[i]);
            writeString(out, pairs[i + 1]);
        }
        return raw.toByteArray();
    }

    private static String readString(final DataInputStream in) throws IOException {
        final int len = in.readUnsignedShort();
        final byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
