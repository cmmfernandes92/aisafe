package eapli.aisafe.app.weather.console.remote.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessEvent;
import eapli.aisafe.app.common.console.remote.logging.RemoteAccessLogger;
import eapli.aisafe.app.weather.console.remote.protocol.ProtocolIO;
import eapli.aisafe.app.weather.console.remote.protocol.WeatherProtocol;
import eapli.aisafe.weather.application.importing.ImportReport;
import eapli.aisafe.weather.application.importing.RowError;
import eapli.aisafe.weather.domain.WeatherData;

/**
 * Handles one TCP client connection. Runs on its own thread. Manages the
 * login state locally (email + password are stored on the stack after a
 * successful LOGIN and passed to AuthGate for every business command).
 */
public class WeatherClientHandler implements Runnable {

    /** US090 service tag for the Weather remote-access service. */
    private static final String SERVICE = "US44";

    private static final int MAX_IMPORT_CHARS = 5_000_000;
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Socket socket;
    private final AuthGate authGate;
    private final WeatherDispatcher dispatcher;
    private final RemoteAccessLogger logger;

    public WeatherClientHandler(final Socket socket) {
        this(socket, new AuthGate(), new WeatherDispatcher(), RemoteAccessLogger.disabled());
    }

    public WeatherClientHandler(final Socket socket, final RemoteAccessLogger logger) {
        this(socket, new AuthGate(), new WeatherDispatcher(), logger);
    }

    WeatherClientHandler(final Socket socket, final AuthGate authGate,
            final WeatherDispatcher dispatcher) {
        this(socket, authGate, dispatcher, RemoteAccessLogger.disabled());
    }

    WeatherClientHandler(final Socket socket, final AuthGate authGate,
            final WeatherDispatcher dispatcher, final RemoteAccessLogger logger) {
        this.socket = socket;
        this.authGate = authGate;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    private void log(final RemoteAccessEvent event, final String username) {
        logger.log(event, username, socket, SERVICE);
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            handleSession(in, out);
        } catch (final IOException e) {
            // client disconnected
        } finally {
            try {
                socket.close();
            } catch (final IOException ignored) {
            }
        }
    }

    void handleSession(final BufferedReader in, final PrintWriter out) throws IOException {
        String loggedInEmail    = null;
        String loggedInPassword = null;

        try {
            String line;
            while ((line = ProtocolIO.readLine(in)) != null) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                final int spaceIdx = trimmed.indexOf(' ');
                final String command = (spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx))
                        .toUpperCase();
                final String args = spaceIdx < 0 ? null
                        : trimmed.substring(spaceIdx + 1).trim();

                switch (command) {
                    case WeatherProtocol.PING:
                        ProtocolIO.sendOk(out, "PONG");
                        break;

                    case WeatherProtocol.LOGIN: {
                        final String[] creds = splitOnPipe(args, 2);
                        if (creds == null) {
                            ProtocolIO.sendErr(out, "BAD_REQUEST");
                            break;
                        }
                        if (authGate.validate(creds[0], creds[1])) {
                            loggedInEmail    = creds[0];
                            loggedInPassword = creds[1];
                            log(RemoteAccessEvent.LOGIN_OK, loggedInEmail);
                            ProtocolIO.sendOk(out);
                        } else {
                            log(RemoteAccessEvent.LOGIN_FAIL, creds[0]);
                            ProtocolIO.sendErr(out,
                                    WeatherProtocol.INVALID_CREDENTIALS_OR_UNAUTHORIZED);
                        }
                        break;
                    }

                    case WeatherProtocol.LOGOUT:
                    case WeatherProtocol.EXIT:
                        if (loggedInEmail != null) {
                            log(RemoteAccessEvent.LOGOUT, loggedInEmail);
                        }
                        loggedInEmail    = null;
                        loggedInPassword = null;
                        ProtocolIO.sendOk(out);
                        return;

                    case WeatherProtocol.AREAS:
                        if (loggedInEmail == null) {
                            ProtocolIO.sendErr(out, WeatherProtocol.LOGIN_REQUIRED);
                        } else {
                            handleAreas(out, loggedInEmail, loggedInPassword);
                        }
                        break;

                    case WeatherProtocol.WCONSULT:
                        if (loggedInEmail == null) {
                            ProtocolIO.sendErr(out, WeatherProtocol.LOGIN_REQUIRED);
                        } else {
                            handleWConsult(out, loggedInEmail, loggedInPassword, args);
                        }
                        break;

                    case WeatherProtocol.WREG:
                        if (loggedInEmail == null) {
                            ProtocolIO.sendErr(out, WeatherProtocol.LOGIN_REQUIRED);
                        } else {
                            handleWReg(out, loggedInEmail, loggedInPassword, args);
                        }
                        break;

                    case WeatherProtocol.WIMPORT:
                        if (loggedInEmail == null) {
                            ProtocolIO.sendErr(out, WeatherProtocol.LOGIN_REQUIRED);
                        } else {
                            handleWImport(in, out, loggedInEmail, loggedInPassword, args);
                        }
                        break;

                    default:
                        ProtocolIO.sendErr(out, "UNKNOWN_COMMAND");
                }
            }
        } finally {
            // loop ended (client closed the socket) or an IOException is propagating,
            // while a session was still active => abnormal disconnect.
            if (loggedInEmail != null) {
                log(RemoteAccessEvent.DISCONNECT, loggedInEmail);
            }
        }
    }

    private void handleAreas(final PrintWriter out,
            final String email, final String password) {
        try {
            final List<AirControlArea> areas = authGate.runAsWeatherPerson(
                    email, password, dispatcher::listAreas);
            ProtocolIO.sendOk(out);
            for (final AirControlArea area : areas) {
                ProtocolIO.sendLine(out, area.code() + " " + area.name());
            }
            ProtocolIO.sendEnd(out);
        } catch (final SecurityException e) {
            ProtocolIO.sendErr(out, e.getMessage());
        } catch (final Exception e) {
            ProtocolIO.sendErr(out, e.getMessage() != null ? e.getMessage() : "INTERNAL_ERROR");
        }
    }

    private void handleWConsult(final PrintWriter out,
            final String email, final String password, final String args) {
        final String[] parts = splitOnPipe(args, 2);
        if (parts == null) {
            ProtocolIO.sendErr(out, "BAD_REQUEST");
            return;
        }
        final String areaCode = parts[0].trim();
        final LocalDate day;
        try {
            day = LocalDate.parse(parts[1].trim());
        } catch (final DateTimeParseException e) {
            ProtocolIO.sendErr(out, "INVALID_DATE_FORMAT");
            return;
        }
        try {
            final List<WeatherData> data = authGate.runAsWeatherPerson(
                    email, password, () -> dispatcher.consultWeather(areaCode, day));
            ProtocolIO.sendOk(out);
            for (final WeatherData wd : data) {
                ProtocolIO.sendLine(out, formatWeatherData(wd));
            }
            ProtocolIO.sendEnd(out);
        } catch (final SecurityException e) {
            ProtocolIO.sendErr(out, e.getMessage());
        } catch (final Exception e) {
            ProtocolIO.sendErr(out, e.getMessage() != null ? e.getMessage() : "INTERNAL_ERROR");
        }
    }

    private static String formatWeatherData(final WeatherData wd) {
        return String.format("%s|%s|%s|temp=%s|pres=%s|vis=%s|wind=%.1f@%.0f|src=%s",
                wd.areaCode(),
                wd.timePeriod().startTime(),
                wd.timePeriod().endTime(),
                wd.temperature() != null ? String.format("%.1f", wd.temperature()) : "N/A",
                wd.pressure()    != null ? String.format("%.1f", wd.pressure())    : "N/A",
                wd.visibility()  != null ? String.format("%.0f", wd.visibility())  : "N/A",
                wd.windSpeed().value(),
                wd.windDirection().value(),
                wd.dataSource());
    }

    private void handleWReg(final PrintWriter out,
            final String email, final String password, final String args) {
        final String[] parts = splitOnPipe(args, 15);
        if (parts == null || parts.length != 15) {
            ProtocolIO.sendErr(out, "BAD_REQUEST");
            return;
        }
        final LocalDateTime start;
        final LocalDateTime end;
        try {
            start = LocalDateTime.parse(parts[1].trim(), DATETIME_FMT);
            end   = LocalDateTime.parse(parts[2].trim(), DATETIME_FMT);
        } catch (final DateTimeParseException e) {
            ProtocolIO.sendErr(out, "INVALID_DATETIME_FORMAT");
            return;
        }
        final double swLat, swLon, neLat, neLon, altLower, altUpper,
                     temp, pres, vis, windSpd, windDir;
        try {
            swLat    = Double.parseDouble(parts[3].trim());
            swLon    = Double.parseDouble(parts[4].trim());
            neLat    = Double.parseDouble(parts[5].trim());
            neLon    = Double.parseDouble(parts[6].trim());
            altLower = Double.parseDouble(parts[7].trim());
            altUpper = Double.parseDouble(parts[8].trim());
            temp     = Double.parseDouble(parts[9].trim());
            pres     = Double.parseDouble(parts[10].trim());
            vis      = Double.parseDouble(parts[11].trim());
            windSpd  = Double.parseDouble(parts[12].trim());
            windDir  = Double.parseDouble(parts[13].trim());
        } catch (final NumberFormatException e) {
            ProtocolIO.sendErr(out, "INVALID_NUMBER_FORMAT");
            return;
        }
        final String areaCode   = parts[0].trim();
        final String dataSource = parts[14].trim();
        try {
            authGate.runAsWeatherPerson(email, password,
                    () -> dispatcher.registerWeather(
                            areaCode, start, end,
                            swLat, swLon, neLat, neLon,
                            altLower, altUpper,
                            temp, pres, vis,
                            windSpd, windDir, dataSource));
            ProtocolIO.sendOk(out);
        } catch (final SecurityException e) {
            ProtocolIO.sendErr(out, e.getMessage());
        } catch (final Exception e) {
            ProtocolIO.sendErr(out, e.getMessage() != null ? e.getMessage() : "INTERNAL_ERROR");
        }
    }

    private void handleWImport(final BufferedReader in, final PrintWriter out,
            final String email, final String password, final String args) throws IOException {
        final String[] parts = splitOnPipe(args, 3);
        if (parts == null || parts.length != 3) {
            ProtocolIO.sendErr(out, "BAD_REQUEST");
            return;
        }
        final String areaCode   = parts[0].trim();
        final String dataSource = parts[1].trim();
        final int charCount;
        try {
            charCount = Integer.parseInt(parts[2].trim());
        } catch (final NumberFormatException e) {
            ProtocolIO.sendErr(out, "BAD_REQUEST");
            return;
        }
        if (charCount < 0) {
            ProtocolIO.sendErr(out, "BAD_REQUEST");
            return;
        }
        if (charCount > MAX_IMPORT_CHARS) {
            ProtocolIO.sendErr(out, "PAYLOAD_TOO_LARGE");
            return;
        }

        // Phase 1 complete — signal client to send body
        ProtocolIO.sendOk(out, "READY");

        // Phase 2 — read exactly charCount characters
        final char[] buf = new char[charCount];
        int totalRead = 0;
        while (totalRead < charCount) {
            final int n = in.read(buf, totalRead, charCount - totalRead);
            if (n == -1) {
                ProtocolIO.sendErr(out, "INCOMPLETE_PAYLOAD");
                throw new IOException("Client closed connection mid-payload");
            }
            totalRead += n;
        }
        final String csvBody = new String(buf);

        try {
            final ImportReport report = authGate.runAsWeatherPerson(email, password,
                    () -> dispatcher.importFromCsv(areaCode, dataSource, csvBody));
            ProtocolIO.sendOk(out, "imported=" + report.imported()
                    + " skipped=" + report.skipped());
            for (final RowError error : report.errors()) {
                ProtocolIO.sendLine(out, error.toString());
            }
            ProtocolIO.sendEnd(out);
        } catch (final SecurityException e) {
            ProtocolIO.sendErr(out, e.getMessage());
        } catch (final Exception e) {
            ProtocolIO.sendErr(out, e.getMessage() != null ? e.getMessage() : "INTERNAL_ERROR");
        }
    }

    private static String[] splitOnPipe(final String s, final int limit) {
        if (s == null || !s.contains("|")) {
            return null;
        }
        final int idx = s.indexOf('|');
        if (limit == 2) {
            return new String[] { s.substring(0, idx), s.substring(idx + 1) };
        }
        return s.split("\\|", limit);
    }
}
