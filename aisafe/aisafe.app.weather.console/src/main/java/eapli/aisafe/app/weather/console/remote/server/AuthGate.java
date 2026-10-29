package eapli.aisafe.app.weather.console.remote.server;

import java.util.function.Supplier;

import eapli.aisafe.app.weather.console.remote.protocol.WeatherProtocol;
import eapli.aisafe.infrastructure.authz.AuthenticationCredentialHandler;
import eapli.aisafe.infrastructure.authz.CredentialHandler;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Guards the shared AuthzRegistry session across concurrent TCP client threads.
 *
 * The eapli framework stores the authenticated user in a static/global session.
 * If two TCP clients authenticate concurrently one session would overwrite the
 * other. This class serialises every interaction with the session through a
 * single JVM-wide lock: authenticate → execute action → clear session, all
 * inside the same synchronized block, so no session ever leaks between threads.
 */
public class AuthGate {

    private static final Object SESSION_LOCK = new Object();

    private final CredentialHandler credentialHandler;

    public AuthGate() {
        this(new AuthenticationCredentialHandler());
    }

    AuthGate(final CredentialHandler credentialHandler) {
        this.credentialHandler = credentialHandler;
    }

    /**
     * Validates credentials for the WEATHER_PERSON role without executing a
     * business action. Used at login time to check credentials before storing
     * them in the handler state.
     *
     * @return {@code true} if valid WEATHER_PERSON credentials, {@code false} otherwise
     */
    public boolean validate(final String email, final String password) {
        synchronized (SESSION_LOCK) {
            try {
                return credentialHandler.authenticated(email, password, AisafeRoles.WEATHER_PERSON);
            } finally {
                AuthzRegistry.authorizationService().clearSession();
            }
        }
    }

    /**
     * Authenticates with the WEATHER_PERSON role, executes {@code action} while
     * the session is active, clears the session in a finally block, then returns
     * the result. The entire sequence runs inside the shared lock so no other
     * TCP client thread can interleave a different session.
     *
     * @param email    the weather person's email
     * @param password the weather person's password
     * @param action   the business operation to execute under the authenticated session
     * @return the value produced by {@code action}
     * @throws SecurityException if the credentials are invalid or the user lacks WEATHER_PERSON role
     */
    public <T> T runAsWeatherPerson(final String email, final String password,
            final Supplier<T> action) {
        synchronized (SESSION_LOCK) {
            if (!credentialHandler.authenticated(email, password, AisafeRoles.WEATHER_PERSON)) {
                throw new SecurityException(WeatherProtocol.INVALID_CREDENTIALS_OR_UNAUTHORIZED);
            }
            try {
                return action.get();
            } finally {
                AuthzRegistry.authorizationService().clearSession();
            }
        }
    }
}
