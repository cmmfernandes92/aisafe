package eapli.aisafe.app.common.console.remote.logging;

/**
 * The four remote-access events logged over UDP to the Remote Accesses Logging
 * Server (US090).
 */
public enum RemoteAccessEvent {
    LOGIN_OK,
    LOGIN_FAIL,
    LOGOUT,
    DISCONNECT
}
