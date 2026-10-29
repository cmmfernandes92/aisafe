package eapli.aisafeweb.web;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.BackofficeQueryService;
import eapli.aisafeweb.service.FlightRouteCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class RoutesMapController {

    private final BackofficeQueryService queries;
    private final FlightRouteCommandService routeCommands;
    private final AisafeWebSession webSession;

    public RoutesMapController(
            final BackofficeQueryService queries,
            final FlightRouteCommandService routeCommands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.routeCommands = routeCommands;
        this.webSession = webSession;
    }

    @GetMapping("/modules/air-transport-company/dashboard")
    public String atccDashboard(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.AIR_TRANSPORT_COMPANY)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.AIR_TRANSPORT_COMPANY, "dashboard");
        model.addAttribute("snapshot", queries.routeMapPage(resolveCompanyIata(session)));
        return "routes-map";
    }

    @GetMapping("/modules/air-transport-company/routes")
    public String routes(final HttpSession session, final Model model) {
        return routesPage(session, model, "routes");
    }

    @GetMapping(AisafeWebModule.ATCC_CREATE_FLIGHT_ROUTE_PATH)
    public String createFlightRoute(final HttpSession session, final Model model) {
        return routesPage(session, model, "create-flight-route");
    }

    @GetMapping(AisafeWebModule.ATCC_DEACTIVATE_FLIGHT_ROUTE_PATH)
    public String deactivateFlightRoute(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.AIR_TRANSPORT_COMPANY)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.AIR_TRANSPORT_COMPANY,
                "deactivate-flight-route");
        model.addAttribute("routes", queries.routeList(resolveCompanyIata(session)));
        return "atcc-deactivate-route";
    }

    private String routesPage(final HttpSession session, final Model model, final String activePage) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.AIR_TRANSPORT_COMPANY)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.AIR_TRANSPORT_COMPANY, activePage);
        model.addAttribute("activePage", activePage);
        model.addAttribute("routes", queries.routeList(resolveCompanyIata(session)));
        model.addAttribute("airports", queries.airports());
        model.addAttribute("companyIata", resolveCompanyIata(session));
        return "atcc-routes";
    }

    @GetMapping("/modules/air-transport-company/routes/table")
    public String routesTable(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("routes", queries.routeList(resolveCompanyIata(session)));
        return "fragments/routes :: rows";
    }

    @PostMapping("/modules/air-transport-company/routes")
    public Object createRoute(
            @RequestParam final String routeName,
            @RequestParam final String originCode,
            @RequestParam final String destinationCode,
            final HttpSession session,
            final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        try {
            webSession.authenticateAtccForControllerCall(session);
            routeCommands.create(routeName.strip(), originCode.strip(), destinationCode.strip());
            model.addAttribute("routes", queries.routeList(resolveCompanyIata(session)));
            return "fragments/routes :: rows";
        } catch (final Exception e) {
            return ResponseEntity.badRequest()
                    .body("<p class=\"form-error\">" + e.getMessage() + "</p>");
        }
    }

    @PostMapping(AisafeWebModule.ATCC_DEACTIVATE_FLIGHT_ROUTE_PATH)
    public String deactivateRoute(
            @RequestParam final String routeName,
            @RequestParam final LocalDate effectiveDate,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateAtccForControllerCall(session);
            routeCommands.deactivate(routeName.strip(), effectiveDate);
            model.addAttribute("success", true);
            model.addAttribute("message", "Flight route deactivated.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        model.addAttribute("routes", queries.routeList(resolveCompanyIata(session)));
        return "fragments/routes :: deactivate-result";
    }

    @GetMapping("/modules/air-transport-company/routes/check-name")
    public ResponseEntity<String> checkRouteName(
            @RequestParam final String routeName,
            final HttpSession session) {
        final String name = routeName.strip();
        if (name.isBlank()) {
            return ResponseEntity.ok("");
        }
        final String companyIata = resolveCompanyIata(session);
        if (companyIata != null && !name.toUpperCase().startsWith(companyIata.toUpperCase())) {
            return ResponseEntity.ok(
                    "<span class=\"field-error\">Name must start with your company IATA code (" + companyIata + ")</span>");
        }
        final boolean exists = PersistenceContext.repositories().flightRoutes()
                .existsByName(eapli.aisafe.flightroutemanagement.domain.RouteName.valueOf(name));
        if (exists) {
            return ResponseEntity.ok("<span class=\"field-error\">Route name already exists</span>");
        }
        return ResponseEntity.ok("<span class=\"field-ok\">Available</span>");
    }

    @GetMapping("/modules/air-transport-company/routes/check-airport")
    public ResponseEntity<String> checkAirport(@RequestParam final String code) {
        final String upper = code.strip().toUpperCase();
        if (upper.isBlank()) {
            return ResponseEntity.ok("");
        }
        try {
            boolean found = false;
            if (upper.length() == 4) {
                found = PersistenceContext.repositories().airports()
                        .findByIcaoCode(new eapli.aisafe.airportmanagement.domain.AirportICAOCode(upper))
                        .isPresent();
            } else if (upper.length() == 3) {
                found = PersistenceContext.repositories().airports()
                        .findByIataCode(new eapli.aisafe.airportmanagement.domain.AirportIATACode(upper))
                        .isPresent();
            }
            if (found) {
                return ResponseEntity.ok("<span class=\"field-ok\">Found</span>");
            }
            return ResponseEntity.ok("<span class=\"field-error\">Airport not found</span>");
        } catch (final Exception e) {
            return ResponseEntity.ok("<span class=\"field-error\">Invalid code</span>");
        }
    }

    private String resolveCompanyIata(final HttpSession session) {
        if (webSession.isPowerUser(session)) {
            return null;
        }
        try {
            final String emailStr = webSession.userEmail(session);
            if (emailStr == null) {
                return null;
            }
            return PersistenceContext.repositories()
                    .customerCollaborators()
                    .findCompanyCollaboratorByEmail(new Email(emailStr))
                    .map(c -> c.companyCode().toString())
                    .orElse(null);
        } catch (final Exception e) {
            return null;
        }
    }
}
