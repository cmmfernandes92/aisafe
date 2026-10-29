package eapli.aisafe.app.weather.console.remote.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import eapli.aisafe.app.weather.console.remote.protocol.WeatherProtocol;

/**
 * Pure TCP text client for Weather Person remote access (US44).
 *
 * This class must NOT import any domain, persistence, JPA, JDBC or controller
 * classes. It communicates exclusively via TCP using the text protocol defined
 * in {@link WeatherProtocol}.
 */
public class WeatherPersonRemoteClient {

    private final Scanner scanner;
    private final PrintStream output;

    public WeatherPersonRemoteClient() {
        this(new Scanner(System.in), System.out);
    }

    WeatherPersonRemoteClient(final Scanner scanner, final PrintStream output) {
        this.scanner = scanner;
        this.output = output;
    }

    public static void main(final String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port    = args.length > 1 ? Integer.parseInt(args[1]) : WeatherProtocol.PORT;
        new WeatherPersonRemoteClient().connect(host, port);
    }

    void connect(final String host, final int port) {
        output.println("  Connecting to " + host + ":" + port + " ...");
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            run(in, out);
        } catch (final IOException e) {
            output.println("  [FAIL] Could not connect: " + e.getMessage());
        }
    }

    void run(final BufferedReader in, final PrintWriter out) throws IOException {
        output.print("  Username: ");
        final String username = scanner.nextLine().trim();
        output.print("  Password: ");
        final String password = scanner.nextLine().trim();

        out.println(WeatherProtocol.LOGIN + " " + username + "|" + password);
        final String loginResp = in.readLine();
        if (loginResp == null || !loginResp.startsWith(WeatherProtocol.OK)) {
            output.println("  [FAIL] " + loginResp);
            return;
        }
        output.println("  [ OK ] Login successful.\n");
        runMenu(in, out);
    }

    void runMenu(final BufferedReader in, final PrintWriter out) throws IOException {
        while (true) {
            printMenu();
            final String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": sendPing(in, out);     break;
                case "2": sendAreas(in, out);    break;
                case "3": sendWConsult(in, out); break;
                case "4": sendWReg(in, out);     break;
                case "5": sendWImport(in, out);  break;
                case "0":
                    sendLogout(in, out);
                    output.println("  Session ended. Bye.");
                    return;
                default:
                    output.println("  Unknown option.");
            }
        }
    }

    void printMenu() {
        output.println();
        output.println("  +" + "-".repeat(40) + "+");
        output.printf("  |  %-38s|%n", "WEATHER PERSON REMOTE ACCESS (US44)");
        output.println("  +" + "-".repeat(40) + "+");
        output.printf("  |  %-38s|%n", "1. Ping server");
        output.printf("  |  %-38s|%n", "2. List Air Control Areas");
        output.printf("  |  %-38s|%n", "3. Consult Weather Data");
        output.printf("  |  %-38s|%n", "4. Register Weather Data");
        output.printf("  |  %-38s|%n", "5. Import Bulk Weather Data (CSV)");
        output.printf("  |  %-38s|%n", "0. Logout");
        output.println("  +" + "-".repeat(40) + "+");
        output.print("  Option: ");
    }

    void sendPing(final BufferedReader in, final PrintWriter out) throws IOException {
        out.println(WeatherProtocol.PING);
        final String resp = in.readLine();
        output.println("  " + (resp != null ? resp : "[no response]"));
    }

    void sendLogout(final BufferedReader in, final PrintWriter out) throws IOException {
        out.println(WeatherProtocol.LOGOUT);
        in.readLine(); // consume OK
    }

    void sendAreas(final BufferedReader in, final PrintWriter out) throws IOException {
        out.println(WeatherProtocol.AREAS);
        final String header = in.readLine();
        if (header == null || !header.startsWith(WeatherProtocol.OK)) {
            output.println("  [FAIL] " + header);
            return;
        }
        output.println("\n  Air Control Areas:");
        String line;
        while ((line = in.readLine()) != null && !WeatherProtocol.END.equals(line)) {
            output.println("    " + line);
        }
    }

    void sendWConsult(final BufferedReader in, final PrintWriter out) throws IOException {
        output.print("  Area code: ");
        final String areaCode = scanner.nextLine().trim();
        output.print("  Date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();

        out.println(WeatherProtocol.WCONSULT + " " + areaCode + "|" + date);
        final String header = in.readLine();
        if (header == null || !header.startsWith(WeatherProtocol.OK)) {
            output.println("  [FAIL] " + header);
            return;
        }
        output.println("\n  Weather data for " + areaCode + " on " + date + ":");
        String line;
        boolean hasData = false;
        while ((line = in.readLine()) != null && !WeatherProtocol.END.equals(line)) {
            output.println("    " + line);
            hasData = true;
        }
        if (!hasData) {
            output.println("    No weather data found.");
        }
    }

    void sendWReg(final BufferedReader in, final PrintWriter out) throws IOException {
        output.print("  Area Code: ");
        final String areaCode = scanner.nextLine().trim();
        output.print("  Period Start (yyyy-MM-dd HH:mm): ");
        final String start = scanner.nextLine().trim();
        output.print("  Period End   (yyyy-MM-dd HH:mm): ");
        final String end = scanner.nextLine().trim();
        output.print("  SW Latitude  (-90..90): ");
        final String swLat = scanner.nextLine().trim();
        output.print("  SW Longitude (-180..180): ");
        final String swLon = scanner.nextLine().trim();
        output.print("  NE Latitude  (-90..90): ");
        final String neLat = scanner.nextLine().trim();
        output.print("  NE Longitude (-180..180): ");
        final String neLon = scanner.nextLine().trim();
        output.print("  Altitude lower (ft, >= 0): ");
        final String altLower = scanner.nextLine().trim();
        output.print("  Altitude upper (ft, > lower): ");
        final String altUpper = scanner.nextLine().trim();
        output.print("  Temperature (°C): ");
        final String temp = scanner.nextLine().trim();
        output.print("  Pressure (hPa): ");
        final String pres = scanner.nextLine().trim();
        output.print("  Visibility (m): ");
        final String vis = scanner.nextLine().trim();
        output.print("  Wind Speed (kt, >= 0): ");
        final String windSpd = scanner.nextLine().trim();
        output.print("  Wind Direction (0-360°): ");
        final String windDir = scanner.nextLine().trim();
        output.print("  Data Source: ");
        final String dataSrc = scanner.nextLine().trim();

        final String payload = String.join("|",
                areaCode, start, end, swLat, swLon, neLat, neLon,
                altLower, altUpper, temp, pres, vis, windSpd, windDir, dataSrc);
        out.println(WeatherProtocol.WREG + " " + payload);
        final String resp = in.readLine();
        output.println("  " + (resp != null ? resp : "[no response]"));
    }

    void sendWImport(final BufferedReader in, final PrintWriter out) throws IOException {
        output.print("  Area Code: ");
        final String areaCode = scanner.nextLine().trim();
        output.print("  Data Source (provider): ");
        final String dataSource = scanner.nextLine().trim();
        output.print("  CSV file path: ");
        final String path = scanner.nextLine().trim();

        final String csvContent;
        try {
            csvContent = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            output.println("  [FAIL] Could not read file: " + e.getMessage());
            return;
        }

        // Phase 1: send metadata only; do not send the body until the server approves.
        out.println(WeatherProtocol.WIMPORT + " " + areaCode + "|" + dataSource
                + "|" + csvContent.length());
        final String preliminary = in.readLine();
        if (preliminary == null || !(WeatherProtocol.OK + " READY").equals(preliminary)) {
            output.println("  [FAIL] " + preliminary);
            return;
        }

        // Phase 2: server validated metadata — now send body and read final result.
        out.print(csvContent);
        out.flush();

        final String header = in.readLine();
        if (header == null || !header.startsWith(WeatherProtocol.OK)) {
            output.println("  [FAIL] " + header);
            return;
        }
        output.println("  " + header);
        String line;
        while ((line = in.readLine()) != null && !WeatherProtocol.END.equals(line)) {
            output.println("  " + line);
        }
    }
}
