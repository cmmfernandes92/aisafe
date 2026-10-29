package eapli.aisafeweb.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AtccPilotCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AtccPilotController {

    private static final String PILOTS_TABLE_PATH = AisafeWebModule.ATCC_PILOTS_PATH + "/table";
    private static final String DEACTIVATE_PATH = AisafeWebModule.ATCC_PILOTS_PATH + "/deactivate";

    private final AtccPilotCommandService pilotCommands;
    private final AisafeWebSession webSession;

    public AtccPilotController(final AtccPilotCommandService pilotCommands,
            final AisafeWebSession webSession) {
        this.pilotCommands = pilotCommands;
        this.webSession = webSession;
    }

    @GetMapping(AisafeWebModule.ATCC_PILOTS_PATH)
    public String pilots(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.AIR_TRANSPORT_COMPANY)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.authenticateAtccForControllerCall(session);
        webSession.addShellAttributes(model, session, AisafeWebModule.AIR_TRANSPORT_COMPANY, "pilots");
        model.addAttribute("snapshot", pilotCommands.pilotPage());
        return "atcc-pilots";
    }

    @GetMapping(PILOTS_TABLE_PATH)
    public String pilotsTable(final HttpSession session, final Model model) {
        webSession.authenticateAtccForControllerCall(session);
        model.addAttribute("pilots", pilotCommands.pilotRows());
        return "fragments/atcc-pilots :: rows";
    }

    @PostMapping(AisafeWebModule.ATCC_PILOTS_PATH)
    public String addPilot(
            @RequestParam final String pilotEmail,
            @RequestParam(required = false) final List<Long> modelIds,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateAtccForControllerCall(session);
            pilotCommands.addPilot(pilotEmail.strip(), modelIds);
            model.addAttribute("success", true);
            model.addAttribute("message", "Pilot added.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        addRows(model);
        return "fragments/atcc-pilots :: action-result";
    }

    @PostMapping(DEACTIVATE_PATH)
    public String deactivatePilot(
            @RequestParam final String pilotEmail,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateAtccForControllerCall(session);
            pilotCommands.deactivatePilot(pilotEmail.strip());
            model.addAttribute("success", true);
            model.addAttribute("message", "Pilot deactivated.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        addRows(model);
        return "fragments/atcc-pilots :: action-result";
    }

    private void addRows(final Model model) {
        try {
            model.addAttribute("pilots", pilotCommands.pilotRows());
        } catch (final RuntimeException ignored) {
            model.addAttribute("pilots", List.of());
        }
    }
}
