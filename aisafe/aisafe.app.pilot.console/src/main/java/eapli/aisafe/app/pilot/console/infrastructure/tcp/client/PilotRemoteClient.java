package eapli.aisafe.app.pilot.console.infrastructure.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol.TcpProtocol;

public class PilotRemoteClient {

    private final Scanner scanner;
    private final PrintStream output;

    public PilotRemoteClient() {
        this(new Scanner(System.in), System.out);
    }

    PilotRemoteClient(final Scanner scanner, final PrintStream output) {
        this.scanner = scanner;
        this.output = output;
    }

    public static void main(final String[] args) throws IOException {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port    = args.length > 1 ? Integer.parseInt(args[1]) : TcpProtocol.PORT;

        new PilotRemoteClient().connect(host, port);
    }

    void connect(final String host, final int port) {
        output.println("  Connecting to " + host + ":" + port + " …");

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream  in  = new DataInputStream(socket.getInputStream())) {

            run(in, out);
        } catch (final IOException e) {
            output.println("  [FAIL] Could not connect: " + e.getMessage());
        }
    }

    void run(final DataInputStream in, final DataOutputStream out) throws IOException {
        output.print("  Username: ");
        final String username = scanner.nextLine().trim();
        output.print("  Password: ");
        final String password = scanner.nextLine().trim();

        if (!sendLogin(out, in, username, password)) {
            output.println("  [FAIL] Login failed.");
            return;
        }
        output.println("  [ OK ] Login successful.\n");

        runMenu(out, in);
    }

    // ------------------------------------------------------------------ login/logout

    boolean sendLogin(final DataOutputStream out, final DataInputStream in,
            final String username, final String password) throws IOException {
        out.writeByte(TcpProtocol.LOGIN);
        writeString(out, username);
        writeString(out, password);
        out.flush();

        final byte response = in.readByte();
        if (response == TcpProtocol.RESPONSE_ERROR) {
            readString(in);
            return false;
        }
        return response == TcpProtocol.RESPONSE_OK;
    }

    void sendLogout(final DataOutputStream out, final DataInputStream in) throws IOException {
        out.writeByte(TcpProtocol.LOGOUT);
        out.flush();
        in.readByte();
    }

    // ------------------------------------------------------------------ menu

    void runMenu(final DataOutputStream out, final DataInputStream in) throws IOException {
        while (true) {
            printMenu();
            final String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": sendCreateFlight(out, in);    break;
                case "2": sendImportPlan(out, in);      break;
                case "3": sendListFlights(out, in);     break;
                case "4": sendAttachWeather(out, in);   break;
                case "5": sendValidatePlan(out, in);    break;
                case "0":
                    sendLogout(out, in);
                    output.println("  Session ended. Bye.");
                    return;
                default:
                    output.println("  Unknown option.");
            }
        }
    }

    void printMenu() {
        output.println();
        output.println("  ┌" + "─".repeat(40) + "┐");
        output.printf("  │  %-38s│%n", "PILOT REMOTE ACCESS");
        output.println("  ├" + "─".repeat(40) + "┤");
        output.printf("  │  %-38s│%n", "1. Create flight (US080)");
        output.printf("  │  %-38s│%n", "2. Import flight plan from DSL (US081)");
        output.printf("  │  %-38s│%n", "3. List my flights");
        output.printf("  │  %-38s│%n", "4. Attach weather to plan (US082)");
        output.printf("  │  %-38s│%n", "5. Validate flight plan (US085)");
        output.printf("  │  %-38s│%n", "0. Logout");
        output.println("  └" + "─".repeat(40) + "┘");
        output.print("  Option: ");
    }

    // ------------------------------------------------------------------ commands

    void sendCreateFlight(final DataOutputStream out,
            final DataInputStream in) throws IOException {
        output.print("  Route name (e.g. TP123): ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Departure date+time (yyyy-MM-ddTHH:mm): ");
        final String departure = scanner.nextLine().trim();
        output.print("  Aircraft registration (e.g. CS-TUA): ");
        final String registration = scanner.nextLine().trim();

        out.writeByte(TcpProtocol.CREATE_FLIGHT_PLAN);
        writeString(out, routeName);
        writeString(out, departure);
        writeString(out, registration);
        out.flush();

        printResponse(in);
    }

    void sendImportPlan(final DataOutputStream out,
            final DataInputStream in) throws IOException {
        output.print("  Route name: ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Departure date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();
        output.println("  Paste DSL content (end with a line containing only '###'):");
        final StringBuilder dsl = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals("###")) {
            dsl.append(line).append('\n');
        }

        out.writeByte(TcpProtocol.CREATE_FROM_DSL);
        writeString(out, routeName);
        writeString(out, date);
        writeString(out, dsl.toString());
        out.flush();

        printResponse(in);
    }

    void sendListFlights(final DataOutputStream out,
            final DataInputStream in) throws IOException {
        out.writeByte(TcpProtocol.LIST_FLIGHTS);
        out.flush();

        final byte code = in.readByte();
        if (code != TcpProtocol.RESPONSE_DATA) {
            output.println("  [FAIL] " + readString(in));
            return;
        }
        final int count = in.readShort();
        output.println("\n  Your flights (" + count + "):");
        for (int i = 0; i < count; i++) {
            final String flightId = readString(in);
            final String status   = readString(in);
            output.printf("    [%d]  %s  plan=%s%n", i + 1, flightId, status);
        }
    }

    void sendAttachWeather(final DataOutputStream out,
            final DataInputStream in) throws IOException {
        output.print("  Route name: ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Departure date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();
        output.print("  Leg number: ");
        final short leg = Short.parseShort(scanner.nextLine().trim());
        output.print("  Segment index: ");
        final short seg = Short.parseShort(scanner.nextLine().trim());
        output.print("  Weather data ID (UUID): ");
        final String weatherId = scanner.nextLine().trim();

        out.writeByte(TcpProtocol.INSERT_WEATHER);
        writeString(out, routeName);
        writeString(out, date);
        out.writeShort(leg);
        out.writeShort(seg);
        writeString(out, weatherId);
        out.flush();

        printResponse(in);
    }

    void sendValidatePlan(final DataOutputStream out,
            final DataInputStream in) throws IOException {
        output.print("  Route name: ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Departure date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();

        out.writeByte(TcpProtocol.VALIDATE_FLIGHT_PLAN);
        writeString(out, routeName);
        writeString(out, date);
        out.flush();

        printResponse(in);
    }

    // ------------------------------------------------------------------ helpers

    void printResponse(final DataInputStream in) throws IOException {
        final byte code = in.readByte();
        if (code == TcpProtocol.RESPONSE_OK) {
            output.println("  [ OK ]");
        } else if (code == TcpProtocol.RESPONSE_DATA) {
            output.println("  [ OK ] " + readString(in));
        } else {
            output.println("  [FAIL] " + readString(in));
        }
    }

    static void writeString(final DataOutputStream out, final String s) throws IOException {
        final byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    static String readString(final DataInputStream in) throws IOException {
        final int len = in.readUnsignedShort();
        final byte[] bytes = new byte[len];
        readFully(in, bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static void readFully(final DataInputStream in, final byte[] buf,
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
