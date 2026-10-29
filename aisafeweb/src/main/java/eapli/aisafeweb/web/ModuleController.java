package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import jakarta.servlet.http.HttpSession;

@Controller
public class ModuleController {

    private final AisafeWebSession webSession;

    public ModuleController(final AisafeWebSession webSession) {
        this.webSession = webSession;
    }

    @GetMapping("/modules/{moduleId}")
    public String switchModule(@PathVariable final String moduleId, final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }

        final AisafeWebModule module = webSession.switchModule(session, moduleId);
        if (module.isBackoffice()) {
            return "redirect:/dashboard";
        }
        if (module == AisafeWebModule.WEATHER_SERVICE) {
            return "redirect:" + module.homePath();
        }
        if (module == AisafeWebModule.AIR_TRANSPORT_COMPANY) {
            return "redirect:" + AisafeWebModule.ATCC_DASHBOARD_PATH;
        }

        webSession.addShellAttributes(model, session, module, "dashboard");
        return "module-dashboard";
    }
}
