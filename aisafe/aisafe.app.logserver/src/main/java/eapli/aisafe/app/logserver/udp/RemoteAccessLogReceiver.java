package eapli.aisafe.app.logserver.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

import eapli.aisafe.app.logserver.store.LogStore;

/**
 * US090 — UDP server side of the Remote Accesses Logging Server. Receives one
 * datagram per remote-access event and appends it to the shared {@link LogStore}.
 *
 * <p>UDP is connectionless: each {@code receive()} yields one self-contained
 * event line. There is no per-client state and no reply.</p>
 */
public final class RemoteAccessLogReceiver implements Runnable {

    private static final int MAX_DATAGRAM = 2048;

    private final int port;
    private final LogStore store;

    public RemoteAccessLogReceiver(final int port, final LogStore store) {
        this.port = port;
        this.store = store;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[LogServer] UDP receiver listening on port " + port);
            final byte[] buffer = new byte[MAX_DATAGRAM];
            final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (!socket.isClosed()) {
                packet.setLength(buffer.length);
                socket.receive(packet);
                final String line = new String(
                        packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                store.record(line);
            }
        } catch (final IOException e) {
            System.err.println("[LogServer] UDP receiver stopped: " + e.getMessage());
        }
    }
}
