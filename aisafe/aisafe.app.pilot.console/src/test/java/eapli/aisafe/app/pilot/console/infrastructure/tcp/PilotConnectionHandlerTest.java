package eapli.aisafe.app.pilot.console.infrastructure.tcp;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;

public class PilotConnectionHandlerTest {

    private static final CredentialHandler ALWAYS_GRANTED = (u, p, roles) -> true;
    private static final CredentialHandler ALWAYS_DENIED  = (u, p, roles) -> false;

    @BeforeClass
    public static void configureAuthzForSocketCtor() {
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
            if (part instanceof Byte) {
                cmd.writeByte((Byte) part);
            } else if (part instanceof Short) {
                cmd.writeShort((Short) part);
            } else if (part instanceof String) {
                writeString(cmd, (String) part);
            }
        }
        return raw.toByteArray();
    }

    private static byte firstResponseByte(final ByteArrayOutputStream rawOut) throws IOException {
        return new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray())).readByte();
    }

    private static PilotConnectionHandler handlerWith(final CredentialHandler credentials,
            final PilotSessionContext sessionContext) {
        return new PilotConnectionHandler(credentials, sessionContext);
    }

    private static void runSession(final byte[] input, final ByteArrayOutputStream rawOut,
            final CredentialHandler handler, final PilotSessionContext sessionContext) throws IOException {
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(input));
        final DataOutputStream out = new DataOutputStream(rawOut);
        handlerWith(handler, sessionContext).handleSession(in, out);
    }

    // ------------------------------------------------------------------ login tests

    @Test
    public void ensureHandlerSendsResponseOkForValidPilotCredentials() throws IOException {
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingSessionContext());

        assertEquals(TcpProtocol.RESPONSE_OK, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseErrorForInvalidCredentials() throws IOException {
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "wrongpass");
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_DENIED, new RecordingSessionContext());

        assertEquals(TcpProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseErrorForNonPilotRole() throws IOException {
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "atcc@test.com", "secret");
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_DENIED, new RecordingSessionContext());

        assertEquals(TcpProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    @Test
    public void ensureHandlerSendsResponseOkFollowedByResponseOkOnLogout() throws IOException {
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingSessionContext());

        final DataInputStream response = new DataInputStream(
                new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, response.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_OK, response.readByte()); // logout
    }

    @Test
    public void ensureHandlerSendsResponseErrorWhenFirstCommandIsNotLogin() throws IOException {
        final byte[] input = buildCommand(TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingSessionContext());

        assertEquals(TcpProtocol.RESPONSE_ERROR, firstResponseByte(rawOut));
    }

    // ------------------------------------------------------------------ dispatch tests

    @Test
    public void ensureCreateFlightCommandReachesSessionContext() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.CREATE_FLIGHT_PLAN, "TP123", "2026-06-15T10:00", "CS-TFX",
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, ctx);

        assertEquals(1, ctx.createFlightCount);
    }

    @Test
    public void ensureCreateFromDslCommandReachesSessionContext() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.CREATE_FROM_DSL, "TP123", "2026-06-15", "FLIGHT_PLAN { }",
                TcpProtocol.LOGOUT);

        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);

        assertEquals(1, ctx.importDslCount);
    }

    @Test
    public void ensureInsertWeatherCommandReachesSessionContext() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.INSERT_WEATHER, "TP123", "2026-06-15",
                (short) 1, (short) 2,
                "11111111-1111-1111-1111-111111111111",
                TcpProtocol.LOGOUT);

        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);

        assertEquals(1, ctx.attachWeatherCount);
    }

    @Test
    public void ensureValidateFlightPlanCommandReachesSessionContext() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.VALIDATE_FLIGHT_PLAN, "TP123", "2026-06-15",
                TcpProtocol.LOGOUT);

        runSession(input, new ByteArrayOutputStream(), ALWAYS_GRANTED, ctx);

        assertEquals(1, ctx.validateCount);
    }

    @Test
    public void ensureListFlightsCommandWritesResponseDataAndCount() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        ctx.flightsForListing.add(SampleFactory.flight("TP123", "CS-TFX", "2026-06-15T10:00"));
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.LIST_FLIGHTS,
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, ctx);

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_DATA, resp.readByte());
        assertEquals(1, resp.readShort());
    }

    @Test
    public void ensureListWeatherCommandWritesResponseDataAndCount() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        ctx.weatherForListing.add(SampleFactory.weather());
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.LIST_WEATHER,
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, ctx);

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_DATA, resp.readByte());
        assertEquals(1, resp.readShort());
    }

    @Test
    public void ensureUnknownCommandWritesResponseErrorAndContinues() throws IOException {
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                (byte) 0x7F,
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, new RecordingSessionContext());

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_ERROR, resp.readByte());
    }

    @Test
    public void ensureIllegalArgumentExceptionFromDispatchIsTranslatedToResponseError() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        ctx.createFlightFailure = new IllegalArgumentException("bad route");
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.CREATE_FLIGHT_PLAN, "TP123", "2026-06-15T10:00", "CS-TFX",
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, ctx);

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_ERROR, resp.readByte());
    }

    @Test
    public void ensureIllegalStateExceptionFromDispatchIsTranslatedToResponseError() throws IOException {
        final RecordingSessionContext ctx = new RecordingSessionContext();
        ctx.createFlightFailure = new IllegalStateException("not ready");
        final byte[] input = buildCommand(
                TcpProtocol.LOGIN, "pilot@test.com", "secret",
                TcpProtocol.CREATE_FLIGHT_PLAN, "TP123", "2026-06-15T10:00", "CS-TFX",
                TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        runSession(input, rawOut, ALWAYS_GRANTED, ctx);

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(rawOut.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_OK, resp.readByte()); // login
        assertEquals(TcpProtocol.RESPONSE_ERROR, resp.readByte());
    }

    // ------------------------------------------------------------------ run() + Socket ctor

    @Test
    public void ensureRunHandlesSessionViaSocketEndToEnd() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();
            final Thread serverThread = new Thread(() -> {
                try {
                    final Socket accepted = server.accept();
                    new PilotConnectionHandler(accepted, ALWAYS_GRANTED, new RecordingSessionContext()).run();
                } catch (final IOException ignored) {
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            try (Socket client = new Socket("localhost", port);
                 DataOutputStream out = new DataOutputStream(client.getOutputStream());
                 DataInputStream in = new DataInputStream(client.getInputStream())) {
                out.writeByte(TcpProtocol.LOGIN);
                writeString(out, "pilot@test.com");
                writeString(out, "secret");
                out.writeByte(TcpProtocol.LOGOUT);
                out.flush();

                assertEquals(TcpProtocol.RESPONSE_OK, in.readByte()); // login
                assertEquals(TcpProtocol.RESPONSE_OK, in.readByte()); // logout
            }
            serverThread.join(1000);
        }
    }

    @Test
    public void ensureRunSwallowsIOExceptionFromClosedSocket() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();
            final Thread serverThread = new Thread(() -> {
                try {
                    final Socket accepted = server.accept();
                    accepted.close();
                    new PilotConnectionHandler(accepted, ALWAYS_GRANTED, new RecordingSessionContext()).run();
                } catch (final IOException ignored) {
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            try (Socket client = new Socket("localhost", port)) {
                // empty
            }
            serverThread.join(1000);
        }
    }

    @Test
    public void ensureSocketCtorBuildsHandlerWithDefaults() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();
            final Thread acceptThread = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    final PilotConnectionHandler handler = new PilotConnectionHandler(accepted);
                    assertNotNull(handler);
                } catch (final IOException ignored) {
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

            try (Socket client = new Socket("localhost", port)) {
                // socket pair established for ctor coverage
            }
            acceptThread.join(1000);
        }
    }

    @Test
    public void ensureRunHandlesNullSocketWithoutClosing() {
        try {
            new PilotConnectionHandler((Socket) null, ALWAYS_GRANTED,
                    new RecordingSessionContext()).run();
        } catch (final NullPointerException expected) {
            // expected — getInputStream() throws on null socket
        }
    }

    @Test
    public void ensureRunSwallowsIOExceptionFromSocketCloseInFinally() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            final int port = server.getLocalPort();
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
                    new PilotConnectionHandler(throwingOnClose, ALWAYS_GRANTED,
                            new RecordingSessionContext()).run();
                } catch (final IOException ignored) {
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            try (Socket client = new Socket("localhost", port);
                 DataOutputStream out = new DataOutputStream(client.getOutputStream());
                 DataInputStream in = new DataInputStream(client.getInputStream())) {
                out.writeByte(TcpProtocol.LOGIN);
                writeString(out, "u");
                writeString(out, "p");
                out.writeByte(TcpProtocol.LOGOUT);
                out.flush();
                assertEquals(TcpProtocol.RESPONSE_OK, in.readByte());
                assertEquals(TcpProtocol.RESPONSE_OK, in.readByte());
            }
            serverThread.join(1000);
        }
    }

    // ------------------------------------------------------------------ readFully + EOF

    @Test
    public void ensureReadFullyReadsAllBytesAcrossPartialReads() throws IOException {
        final PilotConnectionHandler handler = handlerWith(ALWAYS_GRANTED, new RecordingSessionContext());
        final byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(expected));
        final byte[] buf = new byte[expected.length];

        handler.readFully(in, buf, 0, expected.length);

        assertArrayEquals(expected, buf);
    }

    @Test
    public void ensureReadFullyThrowsEofWhenStreamIsClosed() throws IOException {
        final PilotConnectionHandler handler = handlerWith(ALWAYS_GRANTED, new RecordingSessionContext());
        final DataInputStream in = new DataInputStream(new ByteArrayInputStream(new byte[0]));
        final byte[] buf = new byte[4];

        try {
            handler.readFully(in, buf, 0, buf.length);
            fail("Expected EOFException");
        } catch (final EOFException ok) {
            assertTrue(ok.getMessage().contains("Connection closed"));
        }
    }

    // ------------------------------------------------------------------ sendError null

    @Test
    public void ensureSendErrorEmitsDefaultTextWhenMessageIsNull() throws IOException {
        final PilotConnectionHandler handler = handlerWith(ALWAYS_GRANTED, new RecordingSessionContext());
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);

        handler.sendError(out, null);
        out.flush();

        final DataInputStream resp = new DataInputStream(new ByteArrayInputStream(raw.toByteArray()));
        assertEquals(TcpProtocol.RESPONSE_ERROR, resp.readByte());
        final int len = resp.readUnsignedShort();
        final byte[] msg = new byte[len];
        resp.readFully(msg);
        assertEquals("Error", new String(msg, StandardCharsets.UTF_8));
    }

    @Test
    public void ensureDispatchPublicMethodCanBeInvokedDirectly() throws IOException {
        final PilotConnectionHandler handler = handlerWith(ALWAYS_GRANTED, new RecordingSessionContext());
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(raw);

        handler.dispatch((byte) 0x55, new DataInputStream(new ByteArrayInputStream(new byte[0])), out);
        out.flush();

        assertEquals(TcpProtocol.RESPONSE_ERROR, firstResponseByte(raw));
    }

    @Test
    public void ensurePackagePrivateCtorWorksForLoginTests() throws IOException {
        final byte[] input = buildCommand(TcpProtocol.LOGIN, "x", "y", TcpProtocol.LOGOUT);
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        new PilotConnectionHandler(ALWAYS_GRANTED, new RecordingSessionContext()).handleSession(
                new DataInputStream(new ByteArrayInputStream(input)),
                new DataOutputStream(rawOut));

        assertEquals(TcpProtocol.RESPONSE_OK, firstResponseByte(rawOut));
    }

    // ------------------------------------------------------------------ fakes

    private static class RecordingSessionContext implements PilotSessionContext {
        int createFlightCount;
        int importDslCount;
        int attachWeatherCount;
        int validateCount;
        RuntimeException createFlightFailure;
        final List<Flight> flightsForListing = new ArrayList<>();
        final List<WeatherData> weatherForListing = new ArrayList<>();

        @Override
        public Flight createFlight(final RouteName route, final LocalDateTime departure,
                final Registration registration) {
            createFlightCount++;
            if (createFlightFailure != null) {
                throw createFlightFailure;
            }
            return SampleFactory.flight(route.toString(), registration.toString(),
                    departure.toString());
        }

        @Override
        public Flight importPlanFromDsl(final FlightId flightId, final String dslContent) {
            importDslCount++;
            return SampleFactory.flight(flightId.routeName().toString(), "CS-TFX",
                    flightId.departureDate() + "T10:00");
        }

        @Override
        public void attachWeather(final FlightId flightId,
                final Map<SegmentKey, WeatherDataId> assignments) {
            attachWeatherCount++;
        }

        @Override
        public FlightPlanTestResult validateFlightPlan(final FlightId flightId) {
            validateCount++;
            return FlightPlanTestResult.success("ok");
        }

        @Override
        public Iterable<Flight> flightsOfAuthenticatedPilot() {
            return flightsForListing;
        }

        @Override
        public Iterable<WeatherData> allWeatherData() {
            return weatherForListing;
        }
    }

    private static final class SampleFactory {
        static Flight flight(final String routeName, final String registration, final String departure) {
            return new Flight(
                    RouteName.valueOf(routeName),
                    Email.valueOf("pilot@test.com"),
                    Registration.valueOf(registration),
                    LocalDateTime.parse(departure));
        }

        static WeatherData weather() {
            return new WeatherData(
                    AreaCode.valueOf("LPPC"),
                    new TimePeriod(
                            LocalDateTime.of(2026, 6, 15, 6, 0),
                            LocalDateTime.of(2026, 6, 15, 12, 0)),
                    new WeatherZone(
                            new Coordinates(38.0, -10.0),
                            new Coordinates(40.0, -8.0),
                            new AltitudeBand(0.0, 1000.0)),
                    new WeatherWindSpeed(10.0),
                    new WeatherWindDirection(270.0),
                    new DataSource("IPMA"));
        }
    }
}
