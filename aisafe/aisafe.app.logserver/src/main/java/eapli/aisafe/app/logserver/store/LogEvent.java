package eapli.aisafe.app.logserver.store;

/**
 * One remote-access event as received over UDP (US090). Wire format is a single
 * pipe-separated line:
 *
 * <pre>eventType|isoTimestamp|username|clientIp|clientPort|service</pre>
 */
public record LogEvent(
        String type,
        String timestamp,
        String username,
        String ip,
        String clientPort,
        String service) {

    private static final String LOGIN_OK   = "LOGIN_OK";
    private static final String LOGIN_FAIL = "LOGIN_FAIL";
    private static final String LOGOUT     = "LOGOUT";
    private static final String DISCONNECT = "DISCONNECT";

    /** Parses one datagram line; missing fields default to {@code "-"}. */
    public static LogEvent parse(final String line) {
        final String[] f = line.split("\\|", -1);
        return new LogEvent(
                field(f, 0),
                field(f, 1),
                field(f, 2),
                field(f, 3),
                field(f, 4),
                field(f, 5));
    }

    private static String field(final String[] f, final int i) {
        if (i >= f.length) {
            return "-";
        }
        final String v = f[i].trim();
        return v.isEmpty() ? "-" : v;
    }

    /** A successful login — adds the user to the active set. */
    public boolean isLoginOk() {
        return LOGIN_OK.equals(type);
    }

    /** A logout or disconnect — removes the user from the active set. */
    public boolean endsSession() {
        return LOGOUT.equals(type) || DISCONNECT.equals(type);
    }

    /** Key identifying an active remote user (username + service). */
    public String sessionKey() {
        return username + "@" + service;
    }
}
