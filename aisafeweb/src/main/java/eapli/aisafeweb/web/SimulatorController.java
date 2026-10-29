package eapli.aisafeweb.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.FlightSimViewService;
import eapli.aisafeweb.service.PilotQueryService;
import jakarta.servlet.http.HttpSession;

/**
 * Pilot Operations · in-browser flight-plan simulator (v0.2).
 *
 * <p>The pilot picks one of their VALIDATED/TESTED flights; the server recompiles
 * its plan and resolves the aircraft/engine physics, and the client (MapLibre GL
 * + Three.js) animates that real plan. Read-only — it never changes plan state.</p>
 */
@Controller
public class SimulatorController {

    private final AisafeWebSession webSession;
    private final PilotQueryService pilotQueries;
    private final FlightSimViewService simViews;

    public SimulatorController(final AisafeWebSession webSession,
            final PilotQueryService pilotQueries, final FlightSimViewService simViews) {
        this.webSession = webSession;
        this.pilotQueries = pilotQueries;
        this.simViews = simViews;
    }

    @GetMapping("/modules/pilot-operations/simulate-flight-plan")
    public String simulateFlightPlan(
            @RequestParam(required = false) final String route,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.PILOT_OPERATIONS)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.PILOT_OPERATIONS, "simulate-flight-plan");

        final String pilotEmail = webSession.userEmail(session);
        model.addAttribute("simFlights", pilotQueries.simulatableFlightsOf(pilotEmail));
        model.addAttribute("selectedRoute", route);
        model.addAttribute("selectedDate", date == null ? null : date.toString());
        // null when nothing chosen / not eligible — the client then shows the picker only.
        model.addAttribute("plan", simViews.buildFor(pilotEmail, route, date).orElse(null));
        return "pilot-simulate-flight";
    }

    @GetMapping(AisafeWebModule.SIMULATE_AREA_FLIGHTS_PATH)
    public String simulateAreaFlights(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL, "simulate-area-flights");
        return "flight-mgmt-simulate-area";
    }
}