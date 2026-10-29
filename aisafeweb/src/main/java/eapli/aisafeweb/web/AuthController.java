package eapli.aisafeweb.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebSession;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final AisafeWebSession webSession;

    public AuthController(final AisafeWebSession webSession) {
        this.webSession = webSession;
    }

    @GetMapping({"/", "/login"})
    public String login(final HttpSession session) {
        if (webSession.hasLogin(session)) {
            return "redirect:" + webSession.landingPath(session);
        }
        return "login";
    }

    @PostMapping("/login")
    public Object authenticate(
            @RequestParam final String username,
            @RequestParam final String password,
            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
            final HttpSession session,
            final Model model) {
        if (webSession.login(username, password, session)) {
            final String landingPath = webSession.landingPath(session);
            if (hxRequest != null) {
                return ResponseEntity.noContent()
                        .header("HX-Redirect", landingPath)
                        .build();
            }
            return "redirect:" + landingPath;
        }

        model.addAttribute("message", "Invalid credentials or insufficient AISafe module permissions.");
        if (hxRequest != null) {
            return "fragments/auth :: error";
        }
        return "login";
    }

    @PostMapping("/logout")
    public Object logout(
            @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
            final HttpSession session) {
        webSession.logout(session);
        // htmx pages get an HX-Redirect (no full reload); pages without htmx
        // (e.g. the simulators) post natively and need a real redirect.
        if (hxRequest != null) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.LOCATION, "/login")
                    .header("HX-Redirect", "/login")
                    .build();
        }
        return "redirect:/login";
    }
}
