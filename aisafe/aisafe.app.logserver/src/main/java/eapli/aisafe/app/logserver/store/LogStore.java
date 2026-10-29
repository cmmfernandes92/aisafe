package eapli.aisafe.app.logserver.store;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory store of remote-access events, shared between the UDP receiver
 * thread (writer, US090) and the HTTP request threads (readers, US091).
 *
 * <p>Holds a bounded ring buffer of the most recent events plus the set of
 * currently-active remote users. Every accessor is {@code synchronized}: the
 * single intrinsic lock provides the mutual exclusion required for safe
 * cross-thread reads/writes, mirroring the way a concurrent server guards its
 * shared state.</p>
 */
public final class LogStore {

    private static final int DEFAULT_CAPACITY = 100;

    private final int capacity;
    /** Newest first. */
    private final Deque<LogEvent> recent = new ArrayDeque<>();
    /** Active remote users keyed by {@link LogEvent#sessionKey()}, insertion-ordered. */
    private final Map<String, LogEvent> active = new LinkedHashMap<>();

    public LogStore() {
        this(DEFAULT_CAPACITY);
    }

    public LogStore(final int capacity) {
        this.capacity = capacity;
    }

    /** Records one raw datagram line: appends to the ring buffer and updates the active set. */
    public synchronized void record(final String rawLine) {
        final LogEvent event = LogEvent.parse(rawLine);

        recent.addFirst(event);
        while (recent.size() > capacity) {
            recent.removeLast();
        }

        if (event.isLoginOk()) {
            active.put(event.sessionKey(), event);
        } else if (event.endsSession()) {
            active.remove(event.sessionKey());
        }
    }

    // ------------------------------------------------------------------ US091 views

    /** HTML fragment: the most recent events, newest first. */
    public synchronized String eventsHtml() {
        if (recent.isEmpty()) {
            return "<p>No events recorded yet.</p>";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("<table>")
          .append("<tr><th>Event</th><th>Timestamp</th><th>User</th>")
          .append("<th>Client IP</th><th>Port</th><th>Service</th></tr>");
        for (final LogEvent e : recent) {
            sb.append("<tr>")
              .append(td(e.type()))
              .append(td(e.timestamp()))
              .append(td(e.username()))
              .append(td(e.ip()))
              .append(td(e.clientPort()))
              .append(td(e.service()))
              .append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    /** HTML fragment: the currently-active remote users. */
    public synchronized String activeUsersHtml() {
        if (active.isEmpty()) {
            return "<p>No users currently active.</p>";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("<table>")
          .append("<tr><th>User</th><th>Service</th><th>Client IP</th><th>Since</th></tr>");
        for (final LogEvent e : active.values()) {
            sb.append("<tr>")
              .append(td(e.username()))
              .append(td(e.service()))
              .append(td(e.ip()))
              .append(td(e.timestamp()))
              .append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private static String td(final String raw) {
        return "<td>" + escape(raw) + "</td>";
    }

    private static String escape(final String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
