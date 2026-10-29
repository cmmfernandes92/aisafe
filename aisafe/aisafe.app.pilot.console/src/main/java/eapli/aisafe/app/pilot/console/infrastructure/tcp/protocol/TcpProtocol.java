package eapli.aisafe.app.pilot.console.infrastructure.tcp.protocol;

public final class TcpProtocol {

    public static final int PORT = 5555;

    // Client → Server commands
    public static final byte LOGIN                = 0x01;
    public static final byte LOGOUT               = 0x02;
    public static final byte CREATE_FLIGHT_PLAN   = 0x03;
    public static final byte CREATE_FROM_DSL      = 0x04;
    public static final byte INSERT_WEATHER       = 0x05;
    public static final byte VALIDATE_FLIGHT_PLAN = 0x06;
    public static final byte LIST_FLIGHTS         = 0x07;
    public static final byte LIST_WEATHER         = 0x08;

    // Server → Client responses
    public static final byte RESPONSE_OK    = (byte) 0xF0;
    public static final byte RESPONSE_ERROR = (byte) 0xF1;
    public static final byte RESPONSE_DATA  = (byte) 0xF2;

    private TcpProtocol() {
    }
}