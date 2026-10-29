package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;
import eapli.aisafe.fleetmanagement.application.AircraftDTO;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class AtccConnectionHandlerTest {

    private static final CredentialHandler ALWAYS_GRANTED = (u, p, roles) -> true;
    private static final CredentialHandler ALWAYS_DENIED  = (u, p, roles) -> false;

    @BeforeClass
    public static void configureAuthz() {
        AuthzRegistry.configure(new InMemoryUserRepository(),
                new AisafePasswordPolicy(), new PlainTextEncoder());
    }

    // ------------------------------------------------------------------ helpers

    private static void writeString(final DataOutputStream out, final String s) throws IOException {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static byte[] buildCommand(final Object... parts) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream cmd = new DataOutputStream(raw);
        for (final Object part : parts) {
            if (part instanceof Byte)        cmd.writeByte((Byte) part);
            else if (part instanceof Short)  cmd.writeShort((Short) part);
            else if (part instanceof Long)   cmd.writeLong((Long) part);
            else if (part instanceof String) writeString(cmd, (String) part);
        }
        return raw.toByteArray();
    }

    private static byte firstResponseByte(final ByteArrayOutputStream rawOut) throws IOException {
        return new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray())).readByte();
    }

    private static AtccConnectionHandler handlerWith(final CredentialHandler creds,
                                                     final AtccSessionContext ctx) {
        return new AtccConnectionHandler(creds, ctx);
    }

    private static void runSession(final byte[] input, final ByteArrayOutputStream rawOut,
                                   final CredentialHandler creds, final AtccSessionContext ctx) throws IOException {
        final DataInputStream  in  = new DataInputStream(new ByteArrayInputStream(input));
        final DataOutputStream out = new DataOutputStream(rawOut);
        handlerWith(creds, ctx).handleSession(in, out);
    }

    // ------------------------------------------------------------------ login tests

    @Test
    public void ensureHandlerSendsResponseOkForValidAtccCredentials() throws IOException {
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret", AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingCtx());
        assertEquals(AtccProtocol.RESPONSE_OK, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseErrorForInvalidCredentials() throws IOException {
        final byte[] input = buildCommand(AtccProtocol.LOGIN, "atcc@test.com", "wrong");
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_DENIED, new RecordingCtx());
        assertEquals(AtccProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseErrorForNonAtccRole() throws IOException {
        final byte[] input = buildCommand(AtccProtocol.LOGIN, "pilot@test.com", "secret");
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_DENIED, new RecordingCtx());
        assertEquals(AtccProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseOkFollowedByResponseOkOnLogout() throws IOException {
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret", AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingCtx());
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // logout
    }

    @Test
    public void ensureHandlerSendsResponseErrorWhenFirstCommandIsNotLogin() throws IOException {
        final byte[] input = buildCommand(AtccProtocol.LIST_FLEET);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingCtx());
        assertEquals(AtccProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    // ------------------------------------------------------------------ dispatch: ADD_AIRCRAFT (US070)

    @Test
    public void ensureAddAircraftCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.ADD_AIRCRAFT, "CS-TFM",
                (long) 1L, (short) 150, (short) 20, (short) 8, "PT",
                AtccProtocol.LOGOUT);
        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.addAircraftCount);
    }

    @Test
    public void ensureAddAircraftFailureIsTranslatedToResponseError() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        ctx.addAircraftFailure = new IllegalArgumentException("bad model");
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.ADD_AIRCRAFT, "CS-TFM",
                (long) 1L, (short) 150, (short) 20, (short) 8, "PT",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,    resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte()); // error
    }

    // ------------------------------------------------------------------ dispatch: DECOMMISSION (US071)

    @Test
    public void ensureDecommissionCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.DECOMMISSION_AIRCRAFT, "CS-TFM",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.decommissionCount);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // ok
    }

    // ------------------------------------------------------------------ dispatch: LIST_FLEET (US072)

    @Test
    public void ensureListFleetCommandWritesResponseDataAndCount() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        ctx.fleet.add(new AircraftDTO("CS-TFM", "A320", "ACTIVE", 180));
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.LIST_FLEET, AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,   resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_DATA, resp.readByte()); // fleet
        assertEquals(1, resp.readShort());                          // count
    }

    // ------------------------------------------------------------------ dispatch: CREATE_FLIGHT_ROUTE (US073)

    @Test
    public void ensureCreateFlightRouteCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.CREATE_FLIGHT_ROUTE, "TP123", "LIS", "OPO",
                AtccProtocol.LOGOUT);
        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.createRouteCount);
    }

    @Test
    public void ensureCreateFlightRouteFailureIsTranslatedToResponseError() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        ctx.createRouteFailure = new IllegalArgumentException("bad route");
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.CREATE_FLIGHT_ROUTE, "TP123", "LIS", "OPO",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,    resp.readByte());
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte());
    }

    // ------------------------------------------------------------------ dispatch: DEACTIVATE_ROUTE (US074)

    @Test
    public void ensureDeactivateFlightRouteCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.DEACTIVATE_FLIGHT_ROUTE, "TP123", "2026-07-01",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.deactivateRouteCount);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // ok
    }

    // ------------------------------------------------------------------ dispatch: ADD_PILOT (US075)

    @Test
    public void ensureAddPilotCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.ADD_PILOT, "pilot@tap.pt",
                (short) 2, (long) 1L, (long) 2L,
                AtccProtocol.LOGOUT);
        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.addPilotCount);
    }

    @Test
    public void ensureAddPilotFailureIsTranslatedToResponseError() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        ctx.addPilotFailure = new IllegalArgumentException("bad pilot");
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.ADD_PILOT, "pilot@tap.pt",
                (short) 1, (long) 1L,
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,    resp.readByte());
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte());
    }

    // ------------------------------------------------------------------ dispatch: LIST_PILOTS (US076)

    @Test
    public void ensureListPilotsCommandWritesResponseDataAndCount() throws IOException {
        // LIST_PILOTS dispatched -> RESPONSE_DATA + count 0 (empty roster)
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.LIST_PILOTS, AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingCtx());
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,   resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_DATA, resp.readByte()); // pilots
        assertEquals(0, resp.readShort());                          // empty
    }

    // ------------------------------------------------------------------ dispatch: DEACTIVATE_PILOT (US077)

    @Test
    public void ensureDeactivatePilotCommandReachesSessionContext() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.DEACTIVATE_PILOT, "pilot@tap.pt",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        assertEquals(1, ctx.deactivatePilotCount);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_OK, resp.readByte()); // ok
    }

    // ------------------------------------------------------------------ unknown command

    @Test
    public void ensureUnknownCommandWritesResponseErrorAndContinues() throws IOException {
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                (byte) 0x7F, AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingCtx());
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,    resp.readByte()); // login
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte()); // unknown
    }

    @Test
    public void ensureIllegalStateExceptionIsTranslatedToResponseError() throws IOException {
        final RecordingCtx ctx = new RecordingCtx();
        ctx.createRouteFailure = new IllegalStateException("not ready");
        final byte[] input = buildCommand(
                AtccProtocol.LOGIN, "atcc@test.com", "secret",
                AtccProtocol.CREATE_FLIGHT_ROUTE, "TP123", "LIS", "OPO",
                AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        runSession(input, rawOut, ALWAYS_GRANTED, ctx);
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_OK,    resp.readByte());
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte());
    }

    // ------------------------------------------------------------------ run() + Socket ctor

    @Test
    public void ensureRunHandlesSessionViaSocketEndToEnd() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final Thread serverThread = new Thread(() -> {
                try {
                    final Socket accepted = server.accept();
                    new AtccConnectionHandler(accepted, ALWAYS_GRANTED, new RecordingCtx()).run();
                } catch (final IOException ignored) { }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            try (Socket client = new Socket("localhost", server.getLocalPort());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream());
                 DataInputStream  in  = new DataInputStream(client.getInputStream())) {
                out.writeByte(AtccProtocol.LOGIN);
                writeString(out, "atcc@test.com");
                writeString(out, "secret");
                out.writeByte(AtccProtocol.LOGOUT);
                out.flush();
                assertEquals(AtccProtocol.RESPONSE_OK, in.readByte()); // login
                assertEquals(AtccProtocol.RESPONSE_OK, in.readByte()); // logout
            }
            serverThread.join(1000);
        }
    }

    @Test
    public void ensureRunSwallowsIOExceptionFromClosedSocket() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final Thread serverThread = new Thread(() -> {
                try {
                    final Socket accepted = server.accept();
                    accepted.close();
                    new AtccConnectionHandler(accepted, ALWAYS_GRANTED, new RecordingCtx()).run();
                } catch (final IOException ignored) { }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            try (Socket client = new Socket("localhost", server.getLocalPort())) { /* connect */ }
            serverThread.join(1000);
        }
    }

    @Test
    public void ensureSocketCtorBuildsHandlerWithDefaults() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final Thread t = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    assertNotNull(new AtccConnectionHandler(accepted));
                } catch (final IOException ignored) { }
            });
            t.setDaemon(true);
            t.start();
            try (Socket client = new Socket("localhost", server.getLocalPort())) { /* connect */ }
            t.join(1000);
        }
    }

    @Test
    public void ensureRunSwallowsIOExceptionFromSocketCloseInFinally() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final Thread serverThread = new Thread(() -> {
                try {
                    final Socket accepted = server.accept();
                    final Socket throwingOnClose = new Socket() {
                        @Override
                        public java.io.InputStream getInputStream() throws IOException {
                            return accepted.getInputStream();
                        }
                        @Override
                        public java.io.OutputStream getOutputStream() throws IOException {
                            return accepted.getOutputStream();
                        }
                        @Override
                        public synchronized void close() throws IOException {
                            accepted.close();
                            throw new IOException("forced close failure");
                        }
                    };
                    new AtccConnectionHandler(throwingOnClose, ALWAYS_GRANTED, new RecordingCtx()).run();
                } catch (final IOException ignored) { }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            try (Socket client = new Socket("localhost", server.getLocalPort());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream());
                 DataInputStream  in  = new DataInputStream(client.getInputStream())) {
                out.writeByte(AtccProtocol.LOGIN);
                writeString(out, "u");
                writeString(out, "p");
                out.writeByte(AtccProtocol.LOGOUT);
                out.flush();
                assertEquals(AtccProtocol.RESPONSE_OK, in.readByte());
                assertEquals(AtccProtocol.RESPONSE_OK, in.readByte());
            }
            serverThread.join(1000);
        }
    }

    // ------------------------------------------------------------------ readFully

    @Test
    public void ensureReadFullyReadsAllBytesAcrossPartialReads() throws IOException {
        final byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(expected));
        final byte[] buf = new byte[expected.length];
        handlerWith(ALWAYS_GRANTED, new RecordingCtx()).readFully(in, buf, 0, expected.length);
        assertArrayEquals(expected, buf);
    }

    @Test
    public void ensureReadFullyThrowsEofWhenStreamIsClosed() throws IOException {
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(new byte[0]));
        try {
            handlerWith(ALWAYS_GRANTED, new RecordingCtx()).readFully(in, new byte[4], 0, 4);
            fail("Expected EOFException");
        } catch (final EOFException ok) {
            assertTrue(ok.getMessage().contains("Connection closed"));
        }
    }

    // ------------------------------------------------------------------ sendError null

    @Test
    public void ensureSendErrorEmitsDefaultTextWhenMessageIsNull() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        handlerWith(ALWAYS_GRANTED, new RecordingCtx()).sendError(out, null);
        out.flush();
        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(raw.toByteArray()));
        assertEquals(AtccProtocol.RESPONSE_ERROR, resp.readByte());
        final int len = resp.readUnsignedShort();
        final byte[] msg = new byte[len];
        resp.readFully(msg);
        assertEquals("Error", new String(msg, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ dispatch direct

    @Test
    public void ensureDispatchPublicMethodCanBeInvokedDirectly() throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);
        handlerWith(ALWAYS_GRANTED, new RecordingCtx()).dispatch(
                (byte) 0x55,
                new DataInputStream(new ByteArrayInputStream(new byte[0])), out);
        out.flush();
        assertEquals(AtccProtocol.RESPONSE_ERROR, firstResponseByte(raw));
    }

    // ------------------------------------------------------------------ package-private ctor

    @Test
    public void ensurePackagePrivateCtorWorksForLoginTests() throws IOException {
        final byte[] input = buildCommand(AtccProtocol.LOGIN, "x", "y", AtccProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        new AtccConnectionHandler(ALWAYS_GRANTED, new RecordingCtx()).handleSession(
                new DataInputStream(new ByteArrayInputStream(input)),
                new DataOutputStream(rawOut));
        assertEquals(AtccProtocol.RESPONSE_OK, firstResponseByte(rawOut));
    }

    // ------------------------------------------------------------------ recording context

    private static class RecordingCtx implements AtccSessionContext {
        int addAircraftCount;
        int decommissionCount;
        int createRouteCount;
        int deactivateRouteCount;
        int addPilotCount;
        int deactivatePilotCount;
        RuntimeException addAircraftFailure;
        RuntimeException createRouteFailure;
        RuntimeException addPilotFailure;
        final List<AircraftDTO> fleet  = new ArrayList<>();
        final List<Pilot>       pilots = new ArrayList<>();

        @Override
        public Aircraft addAircraft(final String reg, final Long modelId,
                                    final Map<SeatClass, Integer> seats, final String country) {
            addAircraftCount++;
            if (addAircraftFailure != null) throw addAircraftFailure;
            throw new IllegalArgumentException("stub-aircraft");
        }
        @Override
        public Aircraft decommissionAircraft(final String registration) {
            decommissionCount++;
            return null;
        }
        @Override
        public List<AircraftDTO> listFleet() { return fleet; }
        @Override
        public FlightRoute createFlightRoute(final String routeName,
                                             final String originCode, final String destCode) {
            createRouteCount++;
            if (createRouteFailure != null) throw createRouteFailure;
            // always throw so handler catches and sends RESPONSE_ERROR
            // (avoids NPE from route.routeName() on null return)
            throw new IllegalArgumentException("stub-route-" + routeName);
        }
        @Override
        public FlightRoute deactivateFlightRoute(final String routeName,
                                                 final LocalDate effectiveDate) {
            deactivateRouteCount++;
            return null;
        }
        @Override
        public Pilot addPilot(final String pilotEmail, final List<Long> modelIds) {
            addPilotCount++;
            if (addPilotFailure != null) throw addPilotFailure;
            throw new IllegalArgumentException("stub-pilot");
        }
        @Override
        public Iterable<Pilot> listPilots() { return pilots; }
        @Override
        public void deactivatePilot(final String pilotEmail) { deactivatePilotCount++; }
    }
}