package eapli.aisafe.app.atcc.console.infrastructure.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol.AtccProtocol;

public class AtccRemoteClient {

    private final Scanner scanner;
    private final PrintStream output;

    public AtccRemoteClient() {
        this(new Scanner(System.in), System.out);
    }

    AtccRemoteClient(final Scanner scanner, final PrintStream output) {
        this.scanner = scanner;
        this.output = output;
    }

    public static void main(final String[] args) throws IOException {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port    = args.length > 1 ? Integer.parseInt(args[1]) : AtccProtocol.PORT;
        new AtccRemoteClient().connect(host, port);
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
        out.writeByte(AtccProtocol.LOGIN);
        writeString(out, username);
        writeString(out, password);
        out.flush();
        final byte response = in.readByte();
        if (response == AtccProtocol.RESPONSE_ERROR) {
            readString(in);
            return false;
        }
        return response == AtccProtocol.RESPONSE_OK;
    }

    void sendLogout(final DataOutputStream out, final DataInputStream in) throws IOException {
        out.writeByte(AtccProtocol.LOGOUT);
        out.flush();
        in.readByte();
    }

    // ------------------------------------------------------------------ menu

    void runMenu(final DataOutputStream out, final DataInputStream in) throws IOException {
        while (true) {
            printMenu();
            final String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": sendAddAircraft(out, in);            break;
                case "2": sendDecommissionAircraft(out, in);   break;
                case "3": sendListFleet(out, in);              break;
                case "4": sendCreateFlightRoute(out, in);      break;
                case "5": sendDeactivateFlightRoute(out, in);  break;
                case "6": sendAddPilot(out, in);               break;
                case "7": sendListPilots(out, in);             break;
                case "8": sendDeactivatePilot(out, in);        break;
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
        output.println("  ┌" + "─".repeat(42) + "┐");
        output.printf("  │  %-40s│%n", "ATCC REMOTE ACCESS");
        output.println("  ├" + "─".repeat(42) + "┤");
        output.printf("  │  %-40s│%n", "1. Add aircraft (US070)");
        output.printf("  │  %-40s│%n", "2. Decommission aircraft (US071)");
        output.printf("  │  %-40s│%n", "3. List fleet (US072)");
        output.printf("  │  %-40s│%n", "4. Create flight route (US073)");
        output.printf("  │  %-40s│%n", "5. Deactivate flight route (US074)");
        output.printf("  │  %-40s│%n", "6. Add pilot (US075)");
        output.printf("  │  %-40s│%n", "7. List pilots (US076)");
        output.printf("  │  %-40s│%n", "8. Deactivate pilot (US077)");
        output.printf("  │  %-40s│%n", "0. Logout");
        output.println("  └" + "─".repeat(42) + "┘");
        output.print("  Option: ");
    }

    // ------------------------------------------------------------------ commands

    void sendAddAircraft(final DataOutputStream out, final DataInputStream in) throws IOException {
        output.print("  Registration (e.g. CS-TFM): ");
        final String reg = scanner.nextLine().trim();
        output.print("  Aircraft model ID: ");
        final long modelId = Long.parseLong(scanner.nextLine().trim());
        output.print("  Economy seats: ");
        final int eco = Integer.parseInt(scanner.nextLine().trim());
        output.print("  Business seats: ");
        final int bus = Integer.parseInt(scanner.nextLine().trim());
        output.print("  First-class seats: ");
        final int fst = Integer.parseInt(scanner.nextLine().trim());
        output.print("  Registration country (e.g. PT): ");
        final String country = scanner.nextLine().trim();

        out.writeByte(AtccProtocol.ADD_AIRCRAFT);
        writeString(out, reg);
        out.writeLong(modelId);
        out.writeShort(eco);
        out.writeShort(bus);
        out.writeShort(fst);
        writeString(out, country);
        out.flush();
        printResponse(in);
    }

    void sendDecommissionAircraft(final DataOutputStream out,
                                  final DataInputStream in) throws IOException {
        output.print("  Registration to decommission: ");
        final String reg = scanner.nextLine().trim();
        out.writeByte(AtccProtocol.DECOMMISSION_AIRCRAFT);
        writeString(out, reg);
        out.flush();
        printResponse(in);
    }

    void sendListFleet(final DataOutputStream out, final DataInputStream in) throws IOException {
        out.writeByte(AtccProtocol.LIST_FLEET);
        out.flush();
        final byte code = in.readByte();
        if (code != AtccProtocol.RESPONSE_DATA) {
            output.println("  [FAIL] " + readString(in));
            return;
        }
        final int count = in.readShort();
        output.println("\n  Fleet (" + count + " aircraft):");
        for (int i = 0; i < count; i++) {
            final String registration = readString(in);
            final String modelName    = readString(in);
            final String status       = readString(in);
            final int    capacity     = in.readShort();
            output.printf("    [%d]  %-10s  %-20s  %-8s  %d pax%n",
                    i + 1, registration, modelName, status, capacity);
        }
    }

    void sendCreateFlightRoute(final DataOutputStream out,
                               final DataInputStream in) throws IOException {
        output.print("  Route name (e.g. TP123): ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Origin airport code (IATA or ICAO): ");
        final String origin = scanner.nextLine().trim();
        output.print("  Destination airport code (IATA or ICAO): ");
        final String dest = scanner.nextLine().trim();

        out.writeByte(AtccProtocol.CREATE_FLIGHT_ROUTE);
        writeString(out, routeName);
        writeString(out, origin);
        writeString(out, dest);
        out.flush();
        printResponse(in);
    }

    void sendDeactivateFlightRoute(final DataOutputStream out,
                                   final DataInputStream in) throws IOException {
        output.print("  Route name to deactivate: ");
        final String routeName = scanner.nextLine().trim();
        output.print("  Effective date (YYYY-MM-DD): ");
        final String date = scanner.nextLine().trim();

        out.writeByte(AtccProtocol.DEACTIVATE_FLIGHT_ROUTE);
        writeString(out, routeName);
        writeString(out, date);
        out.flush();
        printResponse(in);
    }

    void sendAddPilot(final DataOutputStream out, final DataInputStream in) throws IOException {
        output.print("  Pilot email: ");
        final String email = scanner.nextLine().trim();
        output.print("  Number of aircraft model certifications: ");
        final int count = Integer.parseInt(scanner.nextLine().trim());
        final java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            output.print("    Model ID [" + (i + 1) + "]: ");
            ids.add(Long.parseLong(scanner.nextLine().trim()));
        }

        out.writeByte(AtccProtocol.ADD_PILOT);
        writeString(out, email);
        out.writeShort(count);
        for (final long id : ids) {
            out.writeLong(id);
        }
        out.flush();
        printResponse(in);
    }

    void sendListPilots(final DataOutputStream out, final DataInputStream in) throws IOException {
        out.writeByte(AtccProtocol.LIST_PILOTS);
        out.flush();
        final byte code = in.readByte();
        if (code != AtccProtocol.RESPONSE_DATA) {
            output.println("  [FAIL] " + readString(in));
            return;
        }
        final int count = in.readShort();
        output.println("\n  Pilot roster (" + count + "):");
        for (int i = 0; i < count; i++) {
            final String email   = readString(in);
            final String company = readString(in);
            final String status  = readString(in);
            output.printf("    [%d]  %-30s  %-6s  %s%n", i + 1, email, company, status);
        }
    }

    void sendDeactivatePilot(final DataOutputStream out,
                             final DataInputStream in) throws IOException {
        output.print("  Pilot email to deactivate: ");
        final String email = scanner.nextLine().trim();
        out.writeByte(AtccProtocol.DEACTIVATE_PILOT);
        writeString(out, email);
        out.flush();
        printResponse(in);
    }

    // ------------------------------------------------------------------ helpers

    void printResponse(final DataInputStream in) throws IOException {
        final byte code = in.readByte();
        if (code == AtccProtocol.RESPONSE_OK) {
            output.println("  [ OK ]");
        } else if (code == AtccProtocol.RESPONSE_DATA) {
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
