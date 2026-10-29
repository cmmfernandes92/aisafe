package eapli.aisafeweb.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import jakarta.servlet.http.HttpSession;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class AisafeWebSession {

    public static final String USERNAME = "aisafeweb.username";
    private static final String PASSWORD = "aisafeweb.password";
    private static final String ROLES = "aisafeweb.roles";
    private static final String CURRENT_MODULE = "aisafeweb.currentModule";
    private static final String USER_EMAIL = "aisafeweb.userEmail";

    private static final List<Role> LOGIN_ROLES = List.of(
            AisafeRoles.POWER_USER,
            AisafeRoles.ADMIN,
            AisafeRoles.BACKOFFICE_OPERATOR,
            AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR,
            AisafeRoles.PILOT,
            AisafeRoles.FLIGHT_CONTROL_OPERATOR,
            AisafeRoles.WEATHER_PERSON);

    private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();

    public boolean login(final String username, final String password, final HttpSession session) {
        final Optional<UserSession> userSession = authenticationService.authenticate(username, password);
        final Set<String> authenticatedRoles = userSession
                .map(UserSession::authenticatedUser)
                .map(this::moduleRoleNames)
                .orElseGet(Set::of);
        final boolean authenticated = !authenticatedRoles.isEmpty();

        if (authenticated) {
            session.setAttribute(USERNAME, username);
            session.setAttribute(PASSWORD, password);
            session.setAttribute(ROLES, authenticatedRoles);
            session.setAttribute(CURRENT_MODULE, defaultModule(authenticatedRoles).id());
            userSession.ifPresent(us -> resolveAndStoreEmail(us.authenticatedUser(), session));
        }
        return authenticated;
    }

    public void logout(final HttpSession session) {
        session.invalidate();
    }

    public boolean hasLogin(final HttpSession session) {
        return session != null && session.getAttribute(USERNAME) != null;
    }

    public String username(final HttpSession session) {
        final Object username = session.getAttribute(USERNAME);
        return username == null ? "operator" : username.toString();
    }

    public String userEmail(final HttpSession session) {
        final Object email = session.getAttribute(USER_EMAIL);
        return email == null ? null : email.toString();
    }

    private void resolveAndStoreEmail(final SystemUser systemUser, final HttpSession session) {
        try {
            PersistenceContext.repositories()
                    .aisafeUsers()
                    .findBySystemUser(systemUser)
                    .ifPresent(u -> session.setAttribute(USER_EMAIL, u.identity().toString()));
        } catch (final Exception ignored) {
        }
    }

    public String landingPath(final HttpSession session) {
        return currentModule(session).homePath();
    }

    public AisafeWebModule currentModule(final HttpSession session) {
        final Set<String> roles = roleNames(session);
        final Object stored = session.getAttribute(CURRENT_MODULE);
        if (stored != null) {
            final AisafeWebModule module = moduleOf(stored.toString());
            if (module.isAccessibleBy(roles)) {
                return module;
            }
        }
        final AisafeWebModule fallback = defaultModule(roles);
        session.setAttribute(CURRENT_MODULE, fallback.id());
        return fallback;
    }

    public AisafeWebModule switchModule(final HttpSession session, final String moduleId) {
        if (!hasLogin(session)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        final AisafeWebModule module = moduleOf(moduleId);
        if (!module.isAccessibleBy(roleNames(session))) {
            throw new ResponseStatusException(UNAUTHORIZED, "Module is not available for this user");
        }
        session.setAttribute(CURRENT_MODULE, module.id());
        return module;
    }

    public boolean canAccessModule(final HttpSession session, final AisafeWebModule module) {
        return hasLogin(session) && module.isAccessibleBy(roleNames(session));
    }

    public boolean isPowerUser(final HttpSession session) {
        return hasAnyRole(session, AisafeRoles.POWER_USER);
    }

    public void addShellAttributes(final Model model, final HttpSession session, final AisafeWebModule module,
            final String activePage) {
        session.setAttribute(CURRENT_MODULE, module.id());
        model.addAttribute("username", username(session));
        model.addAttribute("currentModule", module);
        model.addAttribute("modules", moduleOptions(session, module));
        model.addAttribute("navItems", module.navigation(activePage));
    }

    public void authenticateForControllerCall(final HttpSession session) {
        if (!hasLogin(session)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        final String username = String.valueOf(session.getAttribute(USERNAME));
        final String password = String.valueOf(session.getAttribute(PASSWORD));
        if (!hasAnyRole(session, AisafeRoles.POWER_USER, AisafeRoles.ADMIN, AisafeRoles.BACKOFFICE_OPERATOR)
                && !hasAnyRole(session, AisafeRoles.WEATHER_PERSON)
                && !hasAnyRole(session, AisafeRoles.FLIGHT_CONTROL_OPERATOR)
                || authenticationService.authenticate(username, password).isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "AISafe session could not be restored");
        }
    }

    public void authenticateAtccForControllerCall(final HttpSession session) {
        if (!hasLogin(session)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        final String username = String.valueOf(session.getAttribute(USERNAME));
        final String password = String.valueOf(session.getAttribute(PASSWORD));
        if (!hasAnyRole(session, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AisafeRoles.POWER_USER)
                || authenticationService.authenticate(username, password).isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "AISafe session could not be restored");
        }
    }

    public void authenticatePilotForControllerCall(final HttpSession session) {
        if (!hasLogin(session)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        final String username = String.valueOf(session.getAttribute(USERNAME));
        final String password = String.valueOf(session.getAttribute(PASSWORD));
        if (!hasAnyRole(session, AisafeRoles.PILOT, AisafeRoles.POWER_USER)
                || authenticationService.authenticate(username, password).isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "AISafe session could not be restored");
        }
    }

    public void authenticateWeatherConsultForControllerCall(final HttpSession session) {
        if (!hasLogin(session)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }

        final String username = String.valueOf(session.getAttribute(USERNAME));
        final String password = String.valueOf(session.getAttribute(PASSWORD));
        if (!hasAnyRole(session, AisafeRoles.WEATHER_PERSON, AisafeRoles.PILOT,
                AisafeRoles.FLIGHT_CONTROL_OPERATOR)
                || authenticationService.authenticate(username, password).isEmpty()) {
            throw new ResponseStatusException(UNAUTHORIZED, "AISafe session could not be restored");
        }
    }

    private Set<String> moduleRoleNames(final SystemUser user) {
        final Set<String> roles = new LinkedHashSet<>();
        LOGIN_ROLES.stream()
                .filter(user::hasAny)
                .map(Role::toString)
                .forEach(roles::add);
        return roles;
    }

    @SuppressWarnings("unchecked")
    private Set<String> roleNames(final HttpSession session) {
        final Object roles = session.getAttribute(ROLES);
        if (roles instanceof Set<?>) {
            return (Set<String>) roles;
        }
        return Set.of();
    }

    private AisafeWebModule defaultModule(final Set<String> roleNames) {
        return Arrays.stream(AisafeWebModule.values())
                .filter(module -> module.isAccessibleBy(roleNames))
                .findFirst()
                .orElse(AisafeWebModule.BACKOFFICE);
    }

    private AisafeWebModule moduleOf(final String moduleId) {
        return Arrays.stream(AisafeWebModule.values())
                .filter(module -> module.id().equals(moduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unknown AISafe module"));
    }

    private List<AisafeWebModule.ModuleOption> moduleOptions(final HttpSession session,
            final AisafeWebModule activeModule) {
        final Set<String> roles = roleNames(session);
        return Arrays.stream(AisafeWebModule.values())
                .filter(module -> module.isAccessibleBy(roles))
                .map(module -> new AisafeWebModule.ModuleOption(module.id(), module.label(), module.description(),
                        "/modules/" + module.id(), module == activeModule))
                .toList();
    }

    private boolean hasAnyRole(final HttpSession session, final Role... roles) {
        final Set<String> roleNames = roleNames(session);
        return Arrays.stream(roles).map(Role::toString).anyMatch(roleNames::contains);
    }
}
