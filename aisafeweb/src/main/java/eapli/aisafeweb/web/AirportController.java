package eapli.aisafeweb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AirportCommandService;
import eapli.aisafeweb.service.BackofficeQueryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AirportController {

    private final BackofficeQueryService queries;
    private final AirportCommandService commands;
    private final AisafeWebSession webSession;

    public AirportController(
            final BackofficeQueryService queries,
            final AirportCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping("/airports")
    public String airports(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.BACKOFFICE)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.BACKOFFICE, "airports");
        model.addAttribute("snapshot", queries.airportPage());
        return "airports";
    }

    @GetMapping("/airports/table")
    public String airportRows(final Model model) {
        model.addAttribute("airports", queries.airportRows());
        return "fragments/airports :: rows";
    }

    @GetMapping("/airports/check-icao")
    public String checkIcao(@RequestParam(defaultValue = "") final String icaoCode, final Model model) {
        final String cleanCode = icaoCode.trim().toUpperCase();
        String message = "ICAO code is available.";
        boolean valid = true;
        try {
            final AirportICAOCode code = new AirportICAOCode(cleanCode);
            if (PersistenceContext.repositories().airports().findByIcaoCode(code).isPresent()) {
                throw new IllegalArgumentException("Airport ICAO code is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @GetMapping("/airports/check-iata")
    public String checkIata(@RequestParam(defaultValue = "") final String iataCode, final Model model) {
        final String cleanCode = iataCode.trim().toUpperCase();
        String message = "IATA code is available.";
        boolean valid = true;
        try {
            final AirportIATACode code = new AirportIATACode(cleanCode);
            if (PersistenceContext.repositories().airports().findByIataCode(code).isPresent()) {
                throw new IllegalArgumentException("Airport IATA code is already registered.");
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @GetMapping("/airports/check-country")
    public String checkCountry(@RequestParam(defaultValue = "") final String countryCode, final Model model) {
        String message = "Country is registered.";
        boolean valid = true;
        try {
            final CountryCode code = new CountryCode(countryCode);
            if (PersistenceContext.repositories().countries().findByCode(code).isEmpty()) {
                throw new IllegalArgumentException("Country is not registered: " + countryCode);
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    @GetMapping("/airports/check-position")
    public String checkPosition(
            @RequestParam(defaultValue = "") final String areaCode,
            @RequestParam(defaultValue = "NaN") final double latitude,
            @RequestParam(defaultValue = "NaN") final double longitude,
            final Model model) {
        try {
            final AreaCode code = new AreaCode(areaCode);
            final Coordinates coordinates = new Coordinates(latitude, longitude);
            final var area = PersistenceContext.repositories().airControlAreas().findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Air control area is not registered: " + areaCode));
            final boolean contains = area.boundary().contains(coordinates);
            model.addAttribute("valid", contains);
            model.addAttribute("message", contains
                    ? "Coordinates are inside the selected air control area."
                    : "Coordinates are outside the selected air control area.");
        } catch (final IllegalArgumentException ex) {
            model.addAttribute("valid", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: validation";
    }

    @PostMapping("/airports")
    public String createAirport(
            @RequestParam final String icaoCode,
            @RequestParam final String iataCode,
            @RequestParam final String airportName,
            @RequestParam final String town,
            @RequestParam final double elevationMeters,
            @RequestParam final double latitude,
            @RequestParam final double longitude,
            @RequestParam final String countryCode,
            @RequestParam final String areaCode,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(icaoCode, iataCode, airportName, town, elevationMeters,
                    latitude, longitude, countryCode, areaCode);
            model.addAttribute("success", true);
            model.addAttribute("message", "Airport registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }
}
