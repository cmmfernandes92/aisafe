package eapli.aisafeweb.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.FcoAreaQueryService;
import eapli.aisafeweb.service.WeatherConsultService;
import jakarta.servlet.http.HttpSession;

@Controller
public class FlightManagementController {

    private static final String WEATHER_RESULTS_PATH =
            AisafeWebModule.FCO_CONSULT_WEATHER_DATA_PATH + "/results";

    private final AisafeWebSession webSession;
    private final FcoAreaQueryService fcoQueries;
    private final WeatherConsultService weatherConsult;

    public FlightManagementController(final AisafeWebSession webSession,
            final FcoAreaQueryService fcoQueries,
            final WeatherConsultService weatherConsult) {
        this.webSession = webSession;
        this.fcoQueries = fcoQueries;
        this.weatherConsult = weatherConsult;
    }

    /**
     * Flight Control cockpit (the module landing page). Literal path wins over
     * {@code ModuleController}'s generic {@code /modules/{moduleId}}, so the FCO
     * lands here instead of the placeholder module dashboard. Real data: the FCO's
     * own area ({@link FcoAreaQueryService#resolveArea}) and the TESTED flights
     * crossing it that are candidates for an area simulation
     * ({@link FcoAreaQueryService#candidates}). The simulation-outcome and weather
     * panels are placeholders until the egress/US111 pipeline is wired.
     */
    @GetMapping("/modules/flight-control")
    public String dashboard(final HttpSession session, final Model model) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL, "dashboard");

        final String email = webSession.userEmail(session);
        model.addAttribute("area", fcoQueries.resolveArea(email).orElse(null));
        model.addAttribute("candidates", fcoQueries.candidates(email, null, null));
        return "flight-control-dashboard";
    }

    @GetMapping(AisafeWebModule.FCO_CONSULT_WEATHER_DATA_PATH)
    public String consultWeatherData(final HttpSession session, final Model model) {
        final String guard = guardFcoPage(session, model, "consult-weather-data");
        if (guard != null) {
            return guard;
        }
        webSession.authenticateWeatherConsultForControllerCall(session);
        model.addAttribute("areas", weatherConsult.areas());
        model.addAttribute("resultsPath", WEATHER_RESULTS_PATH);
        return "flight-control-consult-weather";
    }

    @GetMapping(WEATHER_RESULTS_PATH)
    public String consultWeatherDataResults(
            @RequestParam(required = false) final String areaCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate day,
            final HttpSession session,
            final Model model) {
        final String guard = guardFcoPage(session, model, "consult-weather-data");
        if (guard != null) {
            return guard;
        }
        webSession.authenticateWeatherConsultForControllerCall(session);
        try {
            model.addAttribute("rows", weatherConsult.consult(areaCode, day));
        } catch (final IllegalArgumentException ex) {
            model.addAttribute("rows", List.of());
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/weather-consult :: rows";
    }

    private String guardFcoPage(final HttpSession session, final Model model, final String activePage) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL)) {
            return "redirect:" + webSession.landingPath(session);
        }

        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL, activePage);
        return null;
    }
}
