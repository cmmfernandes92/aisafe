package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.BackofficeQueryService;
import eapli.aisafeweb.service.CompanyCommandService;
import jakarta.servlet.http.HttpSession;

@Controller
public class CompanyController {

    private final BackofficeQueryService queries;
    private final CompanyCommandService commands;
    private final AisafeWebSession webSession;

    public CompanyController(
            final BackofficeQueryService queries,
            final CompanyCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/companies")
    public String companies(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "companies");
        model.addAttribute("snapshot", queries.companyPage());
        return "companies";
    }

    @GetMapping("/companies/table")
    public String companyRows(final Model model) {
        model.addAttribute("companies", queries.companyRows());
        return "fragments/companies :: rows";
    }

    @GetMapping("/companies/check-iata")
    public String checkIata(@RequestParam(defaultValue = "") final String iataCode, final Model model) {
        final String cleanCode = iataCode.trim().toUpperCase();
        String message = "IATA code is available.";
        boolean valid = true;
        try {
            final CompanyIATACode code = CompanyIATACode.valueOf(cleanCode);
            if (PersistenceContext.repositories().airTransportCompanies().findByIataCode(code).isPresent()) {
                throw new IllegalArgumentException("Company IATA code is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @GetMapping("/companies/check-icao")
    public String checkIcao(@RequestParam(defaultValue = "") final String icaoCode, final Model model) {
        final String cleanCode = icaoCode.trim().toUpperCase();
        String message = "ICAO code is available.";
        boolean valid = true;
        try {
            final CompanyICAOCode code = CompanyICAOCode.valueOf(cleanCode);
            if (PersistenceContext.repositories().airTransportCompanies().findByIcaoCode(code).isPresent()) {
                throw new IllegalArgumentException("Company ICAO code is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @PostMapping("/companies")
    public String createCompany(
            @RequestParam final String iataCode,
            @RequestParam final String icaoCode,
            @RequestParam final String companyName,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(iataCode, icaoCode, companyName);
            model.addAttribute("success", true);
            model.addAttribute("message", "Air transport company registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }
}
