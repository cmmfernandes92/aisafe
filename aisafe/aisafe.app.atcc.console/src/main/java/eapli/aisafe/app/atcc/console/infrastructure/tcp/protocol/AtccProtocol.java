package eapli.aisafe.app.atcc.console.infrastructure.tcp.protocol;

public final class AtccProtocol {

    public static final int PORT = 6666;

    // Client → Server commands
    public static final byte LOGIN                   = 0x01;
    public static final byte LOGOUT                  = 0x02;
    public static final byte ADD_AIRCRAFT            = 0x03;
    public static final byte DECOMMISSION_AIRCRAFT   = 0x04;
    public static final byte LIST_FLEET              = 0x05;
    public static final byte CREATE_FLIGHT_ROUTE     = 0x06;
    public static final byte DEACTIVATE_FLIGHT_ROUTE = 0x07;
    public static final byte ADD_PILOT               = 0x08;
    public static final byte LIST_PILOTS             = 0x09;
    public static final byte DEACTIVATE_PILOT        = 0x0A;

    // Server → Client responses
    public static final byte RESPONSE_OK    = (byte) 0xF0;
    public static final byte RESPONSE_ERROR = (byte) 0xF1;
    public static final byte RESPONSE_DATA  = (byte) 0xF2;

    private AtccProtocol() {
    }
}
