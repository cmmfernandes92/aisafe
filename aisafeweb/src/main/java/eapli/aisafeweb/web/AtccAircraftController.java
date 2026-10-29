package eapli.aisafeweb.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AtccAircraftCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AtccAircraftController {

    private static final String AIRCRAFT_TABLE_PATH = AisafeWebModule.ATCC_AIRCRAFT_PATH + "/table";
    private static final String DECOMMISSION_PATH = AisafeWebModule.ATCC_AIRCRAFT_PATH + "/decommission";

    private final AtccAircraftCommandService aircraftCommands;
    private final AisafeWebSession webSession;

    public AtccAircraftController(final AtccAircraftCommandService aircraftCommands,
            final AisafeWebSession webSession) {
        this.aircraftCommands = aircraftCommands;
        this.webSession = webSession;
    }

    @GetMapping(AisafeWebModule.ATCC_AIRCRAFT_PATH)
    public String aircraft(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.AIR_TRANSPORT_COMPANY)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.authenticateAtccForControllerCall(session);
        webSession.addShellAttributes(model, session, AisafeWebModule.AIR_TRANSPORT_COMPANY, "aircraft");
        model.addAttribute("snapshot", aircraftCommands.aircraftPage());
        return "atcc-aircraft";
    }

    @GetMapping(AIRCRAFT_TABLE_PATH)
    public String aircraftTable(final HttpSession session, final Model model) {
        webSession.authenticateAtccForControllerCall(session);
        model.addAttribute("aircraftRows", aircraftCommands.aircraftRows());
        return "fragments/atcc-aircraft :: rows";
    }

    @PostMapping(AisafeWebModule.ATCC_AIRCRAFT_PATH)
    public String addAircraft(
            @RequestParam final String registration,
            @RequestParam final Long modelId,
            @RequestParam final int economySeats,
            @RequestParam final int businessSeats,
            @RequestParam final int firstClassSeats,
            @RequestParam final String country,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateAtccForControllerCall(session);
            aircraftCommands.addAircraft(registration.strip(), modelId,
                    economySeats, businessSeats, firstClassSeats, country.strip());
            model.addAttribute("success", true);
            model.addAttribute("message", "Aircraft registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        addRows(model);
        return "fragments/atcc-aircraft :: action-result";
    }

    @PostMapping(DECOMMISSION_PATH)
    public String decommissionAircraft(
            @RequestParam final String registration,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateAtccForControllerCall(session);
            aircraftCommands.decommissionAircraft(registration.strip());
            model.addAttribute("success", true);
            model.addAttribute("message", "Aircraft decommissioned.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        addRows(model);
        return "fragments/atcc-aircraft :: action-result";
    }

    private void addRows(final Model model) {
        try {
            model.addAttribute("aircraftRows", aircraftCommands.aircraftRows());
        } catch (final RuntimeException ignored) {
            model.addAttribute("aircraftRows", List.of());
        }
    }
}
