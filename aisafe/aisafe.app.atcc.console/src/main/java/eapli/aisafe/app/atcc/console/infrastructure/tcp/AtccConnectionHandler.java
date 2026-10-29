package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessEvent;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;
import eapli.aisafe.fleetmanagement.application.AircraftDTO;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.AisafeRoles;

public class AtccConnectionHandler implements Runnable {

    /** US090 service tag for the ATCC remote-access service. */
    private static final String SERVICE = "US78";

    private final Socket clientSocket;
    private final CredentialHandler credentialHandler;
    private final AtccSessionContext sessionContext;
    private final RemoteAccessLogger logger;

    public AtccConnectionHandler(final Socket clientSocket) {
        this(clientSocket, new AuthenticationCredentialHandler(),
                new DefaultAtccSessionContext(), RemoteAccessLogger.disabled());
    }

    public AtccConnectionHandler(final Socket clientSocket,
                                 final RemoteAccessLogger logger) {
        this(clientSocket, new AuthenticationCredentialHandler(),
                new DefaultAtccSessionContext(), logger);
    }

    AtccConnectionHandler(final Socket clientSocket,
                          final CredentialHandler credentialHandler,
                          final AtccSessionContext sessionContext) {
        this(clientSocket, credentialHandler, sessionContext, RemoteAccessLogger.disabled());
    }

    AtccConnectionHandler(final CredentialHandler credentialHandler,
                          final AtccSessionContext sessionContext) {
        this(null, credentialHandler, sessionContext, RemoteAccessLogger.disabled());
    }

    AtccConnectionHandler(final Socket clientSocket,
                          final CredentialHandler credentialHandler,
                          final AtccSessionContext sessionContext,
                          final RemoteAccessLogger logger) {
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
        if (cmd != AtccProtocol.LOGIN) {
            sendError(out, "First command must be LOGIN");
            return;
        }

        final String username = readString(in);
        final String password = readString(in);

        if (!credentialHandler.authenticated(username, password,
                AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            log(RemoteAccessEvent.LOGIN_FAIL, username);
            sendError(out, "Unauthorized");
            return;
        }

        log(RemoteAccessEvent.LOGIN_OK, username);
        sendOk(out);

        try {
            byte command;
            while ((command = in.readByte()) != AtccProtocol.LOGOUT) {
                dispatch(command, in, out);
            }
        } catch (final IOException e) {
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
                case AtccProtocol.ADD_AIRCRAFT:
                    handleAddAircraft(in, out);
                    break;
                case AtccProtocol.DECOMMISSION_AIRCRAFT:
                    handleDecommissionAircraft(in, out);
                    break;
                case AtccProtocol.LIST_FLEET:
                    handleListFleet(in, out);
                    break;
                case AtccProtocol.CREATE_FLIGHT_ROUTE:
                    handleCreateFlightRoute(in, out);
                    break;
                case AtccProtocol.DEACTIVATE_FLIGHT_ROUTE:
                    handleDeactivateFlightRoute(in, out);
                    break;
                case AtccProtocol.ADD_PILOT:
                    handleAddPilot(in, out);
                    break;
                case AtccProtocol.LIST_PILOTS:
                    handleListPilots(in, out);
                    break;
                case AtccProtocol.DEACTIVATE_PILOT:
                    handleDeactivatePilot(in, out);
                    break;
                default:
                    sendError(out, "Unknown command: 0x"
                            + Integer.toHexString(commandCode & 0xFF));
            }
        } catch (final IllegalArgumentException | IllegalStateException e) {
            sendError(out, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ US070

    private void handleAddAircraft(final DataInputStream in,
                                   final DataOutputStream out) throws IOException {
        final String registration = readString(in);
        final long modelId = in.readLong();
        // seats: economy, business, first (unsigned shorts)
        final int economy  = in.readUnsignedShort();
        final int business = in.readUnsignedShort();
        final int first    = in.readUnsignedShort();
        final String country = readString(in);

        final Map<SeatClass, Integer> seats = new HashMap<>();
        seats.put(SeatClass.ECONOMY,  economy);
        seats.put(SeatClass.BUSINESS, business);
        seats.put(SeatClass.FIRST,    first);

        final var aircraft = sessionContext.addAircraft(registration, modelId, seats, country);
        sendData(out, aircraft.registration().value());
    }

    // ------------------------------------------------------------------ US071

    private void handleDecommissionAircraft(final DataInputStream in,
                                            final DataOutputStream out) throws IOException {
        final String registration = readString(in);
        sessionContext.decommissionAircraft(registration);
        sendOk(out);
    }

    // ------------------------------------------------------------------ US072

    private void handleListFleet(final DataInputStream in,
                                 final DataOutputStream out) throws IOException {
        final List<AircraftDTO> fleet = sessionContext.listFleet();
        out.writeByte(AtccProtocol.RESPONSE_DATA);
        out.writeShort(fleet.size());
        for (final AircraftDTO dto : fleet) {
            writeString(out, dto.registration());
            writeString(out, dto.modelName());
            writeString(out, dto.status());
            out.writeShort(dto.capacity());
        }
        out.flush();
    }

    // ------------------------------------------------------------------ US073

    private void handleCreateFlightRoute(final DataInputStream in,
                                         final DataOutputStream out) throws IOException {
        final String routeName   = readString(in);
        final String originCode  = readString(in);
        final String destCode    = readString(in);

        final var route = sessionContext.createFlightRoute(routeName, originCode, destCode);
        sendData(out, route.routeName().value());
    }

    // ------------------------------------------------------------------ US074

    private void handleDeactivateFlightRoute(final DataInputStream in,
                                             final DataOutputStream out) throws IOException {
        final String routeName     = readString(in);
        final LocalDate effectiveDate = LocalDate.parse(readString(in));

        sessionContext.deactivateFlightRoute(routeName, effectiveDate);
        sendOk(out);
    }

    // ------------------------------------------------------------------ US075

    private void handleAddPilot(final DataInputStream in,
                                final DataOutputStream out) throws IOException {
        final String pilotEmail = readString(in);
        final int count = in.readUnsignedShort();
        final List<Long> modelIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            modelIds.add(in.readLong());
        }

        final Pilot pilot = sessionContext.addPilot(pilotEmail, modelIds);
        sendData(out, pilot.identity().toString());
    }

    // ------------------------------------------------------------------ US076

    private void handleListPilots(final DataInputStream in,
                                  final DataOutputStream out) throws IOException {
        final List<Pilot> pilots = new ArrayList<>();
        sessionContext.listPilots().forEach(pilots::add);

        out.writeByte(AtccProtocol.RESPONSE_DATA);
        out.writeShort(pilots.size());
        for (final Pilot p : pilots) {
            writeString(out, p.identity().toString());
            writeString(out, p.companyCode().value());
            writeString(out, p.status().name());
        }
        out.flush();
    }

    // ------------------------------------------------------------------ US077

    private void handleDeactivatePilot(final DataInputStream in,
                                       final DataOutputStream out) throws IOException {
        final String pilotEmail = readString(in);
        sessionContext.deactivatePilot(pilotEmail);
        sendOk(out);
    }

    // ------------------------------------------------------------------ wire helpers

    private void sendOk(final DataOutputStream out) throws IOException {
        out.writeByte(AtccProtocol.RESPONSE_OK);
        out.flush();
    }

    void sendError(final DataOutputStream out, final String message) throws IOException {
        out.writeByte(AtccProtocol.RESPONSE_ERROR);
        writeString(out, message != null ? message : "Error");
        out.flush();
    }

    private void sendData(final DataOutputStream out, final String data) throws IOException {
        out.writeByte(AtccProtocol.RESPONSE_DATA);
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
