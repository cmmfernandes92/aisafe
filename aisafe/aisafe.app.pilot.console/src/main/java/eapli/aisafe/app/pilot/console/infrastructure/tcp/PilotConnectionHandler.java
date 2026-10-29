package eapli.aisafe.app.pilot.console.infrastructure.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eapli.aisafe.app.common.console.remote.logging.RemoteAccessEvent;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;
import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;

public class PilotConnectionHandler implements Runnable {

    /** US090 service tag for the Pilot remote-access service. */
    private static final String SERVICE = "US86";

    private final Socket clientSocket;
    private final CredentialHandler credentialHandler;
    private final PilotSessionContext sessionContext;
    private final RemoteAccessLogger logger;

    public PilotConnectionHandler(final Socket clientSocket) {
        this(clientSocket, new AuthenticationCredentialHandler(),
                new DefaultPilotSessionContext(), RemoteAccessLogger.disabled());
    }

    public PilotConnectionHandler(final Socket clientSocket, final RemoteAccessLogger logger) {
        this(clientSocket, new AuthenticationCredentialHandler(),
                new DefaultPilotSessionContext(), logger);
    }

    PilotConnectionHandler(final Socket clientSocket, final CredentialHandler credentialHandler,
            final PilotSessionContext sessionContext) {
        this(clientSocket, credentialHandler, sessionContext, RemoteAccessLogger.disabled());
    }

    PilotConnectionHandler(final CredentialHandler credentialHandler,
            final PilotSessionContext sessionContext) {
        this(null, credentialHandler, sessionContext, RemoteAccessLogger.disabled());
    }

    PilotConnectionHandler(final Socket clientSocket, final CredentialHandler credentialHandler,
            final PilotSessionContext sessionContext, final RemoteAccessLogger logger) {
        this.clientSocket = clientSocket;
        this.credentialHandler = credentialHandler;
        this.sessionContext = sessionContext;
        this.logger = logger;
    }

    private void log(final RemoteAccessEvent event, final String username) {
        logger.log(event, username, clientSocket, SERVICE);
    }

    @Override
    public void run() {
        try (
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            handleSession(in, out);
        } catch (final IOException e) {
            // client disconnected
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (final IOException ignored) {
            }
        }
    }

    void handleSession(final DataInputStream in, final DataOutputStream out) throws IOException {
        final byte cmd = in.readByte();
        if (cmd != TcpProtocol.LOGIN) {
            sendError(out, "First command must be LOGIN");
            return;
        }

        final String username = readString(in);
        final String password = readString(in);

        if (!credentialHandler.authenticated(username, password, AisafeRoles.PILOT)) {
            log(RemoteAccessEvent.LOGIN_FAIL, username);
            sendError(out, "Unauthorized");
            return;
        }

        log(RemoteAccessEvent.LOGIN_OK, username);
        sendOk(out);

        try {
            byte command;
            while ((command = in.readByte()) != TcpProtocol.LOGOUT) {
                dispatch(command, in, out);
            }
        } catch (final IOException e) {
            // the client vanished without a LOGOUT => abnormal disconnect
            log(RemoteAccessEvent.DISCONNECT, username);
            throw e;
        }
        log(RemoteAccessEvent.LOGOUT, username);
        sendOk(out);
    }

    void dispatch(final byte commandCode,
            final DataInputStream in, final DataOutputStream out) throws IOException {
        try {
            switch (commandCode) {
                case TcpProtocol.CREATE_FLIGHT_PLAN:
                    handleCreateFlightPlan(in, out);
                    break;
                case TcpProtocol.CREATE_FROM_DSL:
                    handleCreateFromDsl(in, out);
                    break;
                case TcpProtocol.INSERT_WEATHER:
                    handleInsertWeather(in, out);
                    break;
                case TcpProtocol.VALIDATE_FLIGHT_PLAN:
                    handleValidateFlightPlan(in, out);
                    break;
                case TcpProtocol.LIST_FLIGHTS:
                    handleListFlights(in, out);
                    break;
                case TcpProtocol.LIST_WEATHER:
                    handleListWeather(in, out);
                    break;
                default:
                    sendError(out, "Unknown command: 0x" + Integer.toHexString(commandCode & 0xFF));
            }
        } catch (final IllegalArgumentException | IllegalStateException e) {
            sendError(out, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ US080

    private void handleCreateFlightPlan(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final String routeName   = readString(in);
        final String departureDt = readString(in);
        final String registration = readString(in);

        final Flight flight = sessionContext.createFlight(
                RouteName.valueOf(routeName),
                LocalDateTime.parse(departureDt),
                Registration.valueOf(registration));

        sendData(out, flight.identity().toString());
    }

    // ------------------------------------------------------------------ US081

    private void handleCreateFromDsl(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final String routeName  = readString(in);
        final String date       = readString(in);
        final String dslContent = readString(in);

        final FlightId flightId = new FlightId(
                RouteName.valueOf(routeName), LocalDate.parse(date));

        final Flight flight = sessionContext.importPlanFromDsl(flightId, dslContent);
        sendData(out, flight.currentPlan().status().toString());
    }

    // ------------------------------------------------------------------ US082

    private void handleInsertWeather(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final String routeName = readString(in);
        final String date      = readString(in);
        final int legNumber    = in.readShort();
        final int segmentIndex = in.readShort();
        final String weatherId = readString(in);

        final FlightId flightId = new FlightId(
                RouteName.valueOf(routeName), LocalDate.parse(date));

        final Map<SegmentKey, WeatherDataId> assignments = Map.of(
                new SegmentKey(legNumber, segmentIndex),
                WeatherDataId.valueOf(weatherId));

        sessionContext.attachWeather(flightId, assignments);
        sendOk(out);
    }

    // ------------------------------------------------------------------ US085

    private void handleValidateFlightPlan(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final String routeName = readString(in);
        final String date      = readString(in);

        final FlightId flightId = new FlightId(
                RouteName.valueOf(routeName), LocalDate.parse(date));

        final FlightPlanTestResult result = sessionContext.validateFlightPlan(flightId);
        sendData(out, result.isSuccess() + ":" + result.message());
    }

    // ------------------------------------------------------------------ LIST

    private void handleListFlights(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final List<Flight> flights = new ArrayList<>();
        sessionContext.flightsOfAuthenticatedPilot().forEach(flights::add);

        out.writeByte(TcpProtocol.RESPONSE_DATA);
        out.writeShort(flights.size());
        for (final Flight f : flights) {
            writeString(out, f.identity().toString());
            writeString(out, f.currentPlan().status().toString());
        }
        out.flush();
    }

    private void handleListWeather(final DataInputStream in,
            final DataOutputStream out) throws IOException {
        final List<WeatherData> list = new ArrayList<>();
        sessionContext.allWeatherData().forEach(list::add);

        out.writeByte(TcpProtocol.RESPONSE_DATA);
        out.writeShort(list.size());
        for (final WeatherData w : list) {
            writeString(out, w.identity().toString());
            writeString(out, w.areaCode() + " " + w.timePeriod());
        }
        out.flush();
    }

    // ------------------------------------------------------------------ wire helpers

    private void sendOk(final DataOutputStream out) throws IOException {
        out.writeByte(TcpProtocol.RESPONSE_OK);
        out.flush();
    }

    void sendError(final DataOutputStream out, final String message) throws IOException {
        out.writeByte(TcpProtocol.RESPONSE_ERROR);
        writeString(out, message != null ? message : "Error");
        out.flush();
    }

    private void sendData(final DataOutputStream out, final String data) throws IOException {
        out.writeByte(TcpProtocol.RESPONSE_DATA);
        writeString(out, data);
        out.flush();
    }

    private void writeString(final DataOutputStream out, final String s) throws IOException {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private String readString(final DataInputStream in) throws IOException {
        final int len = in.readUnsignedShort();
        final byte[] bytes = new byte[len];
        readFully(in, bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    void readFully(final DataInputStream in, final byte[] buf,
            final int off, final int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = in.read(buf, off + read, len - read);
            if (r == -1) {
                throw new EOFException("Connection closed");
            }
            read += r;
        }
    }
}
