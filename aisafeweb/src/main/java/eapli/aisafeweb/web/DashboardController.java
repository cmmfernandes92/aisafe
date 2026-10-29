package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.BackofficeQueryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    private final BackofficeQueryService queries;
    private final AisafeWebSession webSession;

    public DashboardController(final BackofficeQueryService queries, final AisafeWebSession webSession) {
        this.queries = queries;
        this.webSession = webSession;
    }

    @GetMapping("/dashboard")
    public String dashboard(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "dashboard");
        model.addAttribute("snapshot", queries.dashboard());
        return "dashboard";
    }
}
