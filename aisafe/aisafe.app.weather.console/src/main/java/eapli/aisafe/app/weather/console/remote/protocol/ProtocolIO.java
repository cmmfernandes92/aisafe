package eapli.aisafe.app.weather.console.remote.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public final class ProtocolIO {

    private ProtocolIO() {
    }

    public static void sendOk(final PrintWriter out) {
        out.println(WeatherProtocol.OK);
        out.flush();
    }

    public static void sendOk(final PrintWriter out, final String message) {
        out.println(WeatherProtocol.OK + " " + message);
        out.flush();
    }

    public static void sendErr(final PrintWriter out, final String token) {
        out.println(WeatherProtocol.ERR + " " + token);
        out.flush();
    }

    public static void sendLine(final PrintWriter out, final String line) {
        out.println(line);
        out.flush();
    }

    public static void sendEnd(final PrintWriter out) {
        out.println(WeatherProtocol.END);
        out.flush();
    }

    public static String readLine(final BufferedReader in) throws IOException {
        return in.readLine();
    }
}
