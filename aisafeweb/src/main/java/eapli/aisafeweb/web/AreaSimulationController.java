package eapli.aisafeweb.web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationRun;
import eapli.aisafe.flightmanagement.application.areasimulation.SimulationWindow;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AreaSimulationCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AreaSimulationController {

    private static final String ACTIVE_PAGE = "run-area-simulation";
    private static final DateTimeFormatter INPUT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AisafeWebSession webSession;
    private final AreaSimulationCommandService areaSimulation;

    public AreaSimulationController(final AisafeWebSession webSession,
            final AreaSimulationCommandService areaSimulation) {
        this.webSession = webSession;
        this.areaSimulation = areaSimulation;
    }

    @GetMapping(AisafeWebModule.RUN_AREA_SIMULATION_PATH)
    public String runAreaSimulation(final HttpSession session, final Model model) {
        final String guard = guardFlightControlPage(session, model);
        if (guard != null) {
            return guard;
        }

        final SimulationWindow window = defaultWindow();
        addWindowAttributes(model, session, window);
        return "flight-control-run-area-sim";
    }

    @GetMapping(AisafeWebModule.RUN_AREA_SIMULATION_PATH + "/candidates")
    public String candidates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime to,
            final HttpSession session,
            final Model model) {
        final String guard = guardFlightControlFragment(session);
        if (guard != null) {
            return guard;
        }

        try {
            final SimulationWindow window = windowOrDefault(from, to);
            addCandidateAttributes(model, session, window);
        } catch (final IllegalArgumentException e) {
            model.addAttribute("area", areaSimulation.area(webSession.userEmail(session)).orElse(null));
            model.addAttribute("candidates", List.of());
            model.addAttribute("message", e.getMessage());
        }
        return "fragments/area-simulation :: candidateRows";
    }

    @PostMapping(AisafeWebModule.RUN_AREA_SIMULATION_PATH)
    public String runAreaSimulation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime to,
            final HttpSession session,
            final Model model) {
        final String guard = guardFlightControlFragment(session);
        if (guard != null) {
            return guard;
        }

        try {
            webSession.authenticateForControllerCall(session);
            final AreaSimulationRun run = areaSimulation.run(windowOrDefault(from, to));
            model.addAttribute("success", true);
            model.addAttribute("message", "Simulation ran successfully — outputs written to disk.");
            model.addAttribute("run", run);
            // Path methods are blocked inside Thymeleaf (SpringEL sandbox), so resolve
            // the run folder to a plain string here.
            model.addAttribute("runDir", run.runDir().toAbsolutePath().toString());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            addFailure(model, "Simulation interrupted.");
        } catch (final IOException e) {
            addFailure(model, "Could not write or run the simulation: " + e.getMessage());
        } catch (final RuntimeException e) {
            addFailure(model, friendlyMessage(e));
        }
        return "fragments/area-simulation :: result";
    }

    private void addWindowAttributes(final Model model, final HttpSession session, final SimulationWindow window) {
        model.addAttribute("from", INPUT_DATETIME.format(window.from()));
        model.addAttribute("to", INPUT_DATETIME.format(window.to()));
        addCandidateAttributes(model, session, window);
    }

    private void addCandidateAttributes(final Model model, final HttpSession session, final SimulationWindow window) {
        final String email = webSession.userEmail(session);
        model.addAttribute("area", areaSimulation.area(email).orElse(null));
        model.addAttribute("candidates", areaSimulation.candidates(email, window));
    }

    private static SimulationWindow defaultWindow() {
        // Match the FCO dashboard's "next 24h" candidate window so the count the FCO
        // sees on the dashboard matches the candidates listed here.
        final LocalDateTime from = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        return new SimulationWindow(from, from.plusHours(24));
    }

    private static SimulationWindow windowOrDefault(final LocalDateTime from, final LocalDateTime to) {
        if (from == null && to == null) {
            return defaultWindow();
        }
        return new SimulationWindow(
                from == null ? defaultWindow().from() : from,
                to == null ? defaultWindow().to() : to);
    }

    private String guardFlightControlPage(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL, ACTIVE_PAGE);
        return null;
    }

    private String guardFlightControlFragment(final HttpSession session) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL)) {
            return "redirect:" + webSession.landingPath(session);
        }
        return null;
    }

    private static void addFailure(final Model model, final String message) {
        model.addAttribute("success", false);
        model.addAttribute("message", message);
        model.addAttribute("run", null);
    }

    private static String friendlyMessage(final RuntimeException e) {
        final String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (message.startsWith("No TESTED candidate flights")) {
            return "No TESTED candidate flights cross your area in that window.";
        }
        if (message.startsWith("SCOMP flight_sim binary not found or not executable")) {
            return "SCOMP flight_sim is missing or not executable. Run `make` in the flight_sim directory, then retry. "
                    + message;
        }
        return message;
    }
}
