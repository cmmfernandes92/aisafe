package eapli.aisafe.app.weather.console.remote.protocol;

public final class WeatherProtocol {

    public static final int PORT = 5556;

    // Client → Server commands
    public static final String PING     = "PING";
    public static final String LOGIN    = "LOGIN";
    public static final String LOGOUT   = "LOGOUT";
    public static final String EXIT     = "EXIT";
    public static final String AREAS    = "AREAS";
    public static final String WCONSULT = "WCONSULT";
    public static final String WREG     = "WREG";
    public static final String WIMPORT  = "WIMPORT";

    // Server → Client response prefixes
    public static final String OK  = "OK";
    public static final String ERR = "ERR";
    public static final String END = "END";

    // Error tokens
    public static final String LOGIN_REQUIRED                      = "LOGIN_REQUIRED";
    public static final String INVALID_CREDENTIALS_OR_UNAUTHORIZED = "INVALID_CREDENTIALS_OR_UNAUTHORIZED";

    private WeatherProtocol() {
    }
}
