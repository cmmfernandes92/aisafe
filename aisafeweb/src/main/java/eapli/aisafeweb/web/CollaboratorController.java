package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.BackofficeQueryService;
import eapli.aisafeweb.service.CollaboratorCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class CollaboratorController {

    private final BackofficeQueryService queries;
    private final CollaboratorCommandService commands;
    private final AisafeWebSession webSession;

    public CollaboratorController(
            final BackofficeQueryService queries,
            final CollaboratorCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/collaborators")
    public String collaborators(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "collaborators");
        model.addAttribute("snapshot", queries.collaboratorPage());
        return "collaborators";
    }

    @GetMapping("/collaborators/table")
    public String collaboratorRows(final Model model) {
        model.addAttribute("collaborators", queries.collaboratorRows());
        return "fragments/collaborators :: rows";
    }

    @PostMapping("/collaborators/company")
    public String createCompanyCollaborator(
            @RequestParam final String email,
            @RequestParam final String position,
            @RequestParam final String companyCode,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.registerCompanyCollaborator(email, position, companyCode);
            model.addAttribute("success", true);
            model.addAttribute("message", "Company collaborator registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    @PostMapping("/collaborators/area")
    public String createAreaCollaborator(
            @RequestParam final String email,
            @RequestParam final String position,
            @RequestParam final String areaCode,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.registerAreaCollaborator(email, position, areaCode);
            model.addAttribute("success", true);
            model.addAttribute("message", "Area collaborator registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }
}
