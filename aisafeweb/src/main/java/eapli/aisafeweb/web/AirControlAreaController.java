package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AirControlAreaCommandService;
import eapli.aisafeweb.service.BackofficeQueryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AirControlAreaController {

    private final BackofficeQueryService queries;
    private final AirControlAreaCommandService commands;
    private final AisafeWebSession webSession;

    public AirControlAreaController(
            final BackofficeQueryService queries,
            final AirControlAreaCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/areas")
    public String areas(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "air-control-areas");
        model.addAttribute("snapshot", queries.areaPage());
        return "areas";
    }

    @GetMapping("/areas/table")
    public String areaRows(final Model model) {
        model.addAttribute("areas", queries.areas());
        return "fragments/areas :: rows";
    }

    @GetMapping("/areas/check-code")
    public String checkCode(@RequestParam(defaultValue = "") final String code, final Model model) {
        final String cleanCode = code.trim().toUpperCase();
        String message = "Code is available.";
        boolean valid = true;
        try {
            if (cleanCode.isBlank()) {
                throw new IllegalArgumentException("Area code is required.");
            }
            final AreaCode areaCode = new AreaCode(cleanCode);
            if (PersistenceContext.repositories().airControlAreas().findByCode(areaCode).isPresent()) {
                throw new IllegalArgumentException("Area code is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @GetMapping("/areas/check-name")
    public String checkName(@RequestParam(defaultValue = "") final String name, final Model model) {
        final String cleanName = name.trim();
        String message = "Name is available.";
        boolean valid = true;
        try {
            if (cleanName.isBlank()) {
                throw new IllegalArgumentException("Area name is required.");
            }
            final AreaName areaName = new AreaName(cleanName);
            if (PersistenceContext.repositories().airControlAreas().findByName(areaName).isPresent()) {
                throw new IllegalArgumentException("Area name is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @PostMapping("/areas")
    public String createArea(
            @RequestParam final String code,
            @RequestParam final String name,
            @RequestParam final double swLat,
            @RequestParam final double swLon,
            @RequestParam final double neLat,
            @RequestParam final double neLon,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(code, name, swLat, swLon, neLat, neLon);
            model.addAttribute("success", true);
            model.addAttribute("message", "Air control area registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    @GetMapping("/areas/check-boundary")
    public String checkBoundary(
            @RequestParam final double swLat,
            @RequestParam final double swLon,
            @RequestParam final double neLat,
            @RequestParam final double neLon,
            final Model model) {
        try {
            final GeographicBoundary boundary = new GeographicBoundary(
                    new Coordinates(swLat, swLon),
                    new Coordinates(neLat, neLon));
            final boolean overlaps = PersistenceContext.repositories().airControlAreas()
                    .findOverlapping(boundary)
                    .iterator()
                    .hasNext();
            model.addAttribute("valid", !overlaps);
            model.addAttribute("message", overlaps
                    ? "Boundary overlaps an existing air control area."
                    : "Boundary is valid.");
        } catch (final IllegalArgumentException ex) {
            model.addAttribute("valid", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: validation";
    }
}
