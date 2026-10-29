package eapli.aisafeweb.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AircraftModelCommandService;
import eapli.aisafeweb.service.BackofficeQueryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AircraftModelController {

    private final BackofficeQueryService queries;
    private final AircraftModelCommandService commands;
    private final AisafeWebSession webSession;

    public AircraftModelController(
            final BackofficeQueryService queries,
            final AircraftModelCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/aircraft-models")
    public String aircraftModels(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "aircraft-models");
        model.addAttribute("snapshot", queries.aircraftModelPage());
        model.addAttribute("aircraftTypes", AircraftType.values());
        model.addAttribute("motorizationTypes", MotorizationType.values());
        return "aircraft-models";
    }

    @GetMapping("/aircraft-models/table")
    public String aircraftModelRows(final Model model) {
        model.addAttribute("aircraftModels", queries.aircraftModelRows());
        return "fragments/aircraft-models :: rows";
    }

    @GetMapping("/aircraft-models/engines-by-motorization")
    public String enginesByMotorizationType(
            @RequestParam(defaultValue = "") final String motorizationType,
            final Model model) {
        try {
            final MotorizationType type = MotorizationType.valueOf(motorizationType);
            model.addAttribute("engineModels", queries.engineModelsByMotorizationType(type));
        } catch (final IllegalArgumentException e) {
            model.addAttribute("engineModels", List.of());
        }
        return "fragments/aircraft-models :: engine-options";
    }

    @PostMapping("/aircraft-models")
    public String createAircraftModel(
            @RequestParam final String modelName,
            @RequestParam final String makerName,
            @RequestParam final AircraftType type,
            @RequestParam final MotorizationType motorizationType,
            @RequestParam final double emptyWeight,
            @RequestParam final double mtow,
            @RequestParam final double mzfw,
            @RequestParam final double maxFuelCapacity,
            @RequestParam final double serviceCeiling,
            @RequestParam final double cruiseSpeed,
            @RequestParam final double wingArea,
            @RequestParam final double dragCoefficient,
            @RequestParam final double liftCoefficient,
            @RequestParam final double maximumRange,
            @RequestParam(required = false) final List<Long> engineIds,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(modelName, makerName, type, motorizationType,
                    emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                    wingArea, dragCoefficient, liftCoefficient, maximumRange, engineIds);
            model.addAttribute("success", true);
            model.addAttribute("message", "Aircraft model registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }
}
