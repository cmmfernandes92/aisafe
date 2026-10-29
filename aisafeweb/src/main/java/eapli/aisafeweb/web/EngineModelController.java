package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.BackofficeQueryService;
import eapli.aisafeweb.service.EngineModelCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class EngineModelController {

    private final BackofficeQueryService queries;
    private final EngineModelCommandService commands;
    private final AisafeWebSession webSession;

    public EngineModelController(
            final BackofficeQueryService queries,
            final EngineModelCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/engine-models")
    public String engineModels(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "engine-models");
        model.addAttribute("snapshot", queries.engineModelPage());
        model.addAttribute("motorizationTypes", MotorizationType.values());
        model.addAttribute("fuelTypes", FuelType.values());
        return "engine-models";
    }

    @GetMapping("/engine-models/table")
    public String engineModelRows(final Model model) {
        model.addAttribute("engineModels", queries.engineModelRows());
        return "fragments/engine-models :: rows";
    }

    @PostMapping("/engine-models")
    public String createEngineModel(
            @RequestParam final String engineModelName,
            @RequestParam final String makerName,
            @RequestParam final MotorizationType motorizationType,
            @RequestParam final FuelType fuelType,
            @RequestParam final double power,
            @RequestParam final double efficiency,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(engineModelName, makerName, motorizationType, fuelType, power, efficiency);
            model.addAttribute("success", true);
            model.addAttribute("message", "Engine model registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }
}
