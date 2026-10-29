package eapli.aisafeweb.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.WeatherCommandService;
import eapli.aisafeweb.service.WeatherConsultService;
import eapli.aisafeweb.service.WeatherQueryService;
import jakarta.servlet.http.HttpSession;

@Controller
public class WeatherController {

    private static final String CONSULT_RESULTS_PATH =
            AisafeWebModule.WEATHER_CONSULT_DATA_PATH + "/results";

    private final WeatherQueryService queries;
    private final WeatherConsultService weatherConsult;
    private final WeatherCommandService commands;
    private final AisafeWebSession webSession;

    public WeatherController(
            final WeatherQueryService queries,
            final WeatherConsultService weatherConsult,
            final WeatherCommandService commands,
            final AisafeWebSession webSession) {
        this.queries = queries;
        this.weatherConsult = weatherConsult;
        this.commands = commands;
        this.webSession = webSession;
    }

    @GetMapping(AisafeWebModule.WEATHER_DASHBOARD_PATH)
    public String dashboard(final HttpSession session, final Model model) {
        if (!canAccessWeather(session)) {
            return redirectToLoginOrLanding(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.WEATHER_SERVICE, "dashboard");
        model.addAttribute("snapshot", queries.dashboard());
        return "weather-dashboard";
    }

    @GetMapping(AisafeWebModule.REGISTER_WEATHER_DATA_PATH)
    public String registerWeatherData(final HttpSession session, final Model model) {
        if (!canAccessWeather(session)) {
            return redirectToLoginOrLanding(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.WEATHER_SERVICE, "register-weather-data");
        model.addAttribute("snapshot", queries.weatherDataPage());
        return "weather-register";
    }

    @PostMapping(AisafeWebModule.REGISTER_WEATHER_DATA_PATH)
    public String createWeatherData(
            @RequestParam final String areaCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime,
            @RequestParam final double zoneSwLat,
            @RequestParam final double zoneSwLon,
            @RequestParam final double zoneNeLat,
            @RequestParam final double zoneNeLon,
            @RequestParam final double altLowerFeet,
            @RequestParam final double altUpperFeet,
            @RequestParam(required = false) final Double temperature,
            @RequestParam(required = false) final Double pressure,
            @RequestParam(required = false) final Double visibility,
            @RequestParam final double windSpeed,
            @RequestParam final double windDirection,
            @RequestParam final String dataSource,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            commands.register(areaCode, startTime, endTime,
                    zoneSwLat, zoneSwLon, zoneNeLat, zoneNeLon,
                    altLowerFeet, altUpperFeet,
                    temperature, pressure, visibility,
                    windSpeed, windDirection, dataSource);
            model.addAttribute("success", true);
            model.addAttribute("message", "Weather data registered.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    @GetMapping(AisafeWebModule.IMPORT_BULK_WEATHER_DATA_PATH)
    public String importBulkWeatherData(final HttpSession session, final Model model) {
        if (!canAccessWeather(session)) {
            return redirectToLoginOrLanding(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.WEATHER_SERVICE, "import-bulk-data");
        model.addAttribute("snapshot", queries.weatherDataPage());
        return "weather-import";
    }

    @GetMapping(AisafeWebModule.WEATHER_CONSULT_DATA_PATH)
    public String consultWeatherData(final HttpSession session, final Model model) {
        if (!canAccessWeather(session)) {
            return redirectToLoginOrLanding(session);
        }
        webSession.authenticateWeatherConsultForControllerCall(session);
        webSession.addShellAttributes(model, session, AisafeWebModule.WEATHER_SERVICE, "consult-weather-data");
        model.addAttribute("areas", weatherConsult.areas());
        model.addAttribute("resultsPath", CONSULT_RESULTS_PATH);
        return "weather-consult";
    }

    @GetMapping(CONSULT_RESULTS_PATH)
    public String consultWeatherDataResults(
            @RequestParam(required = false) final String areaCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate day,
            final HttpSession session,
            final Model model) {
        if (!canAccessWeather(session)) {
            return redirectToLoginOrLanding(session);
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

    @PostMapping(AisafeWebModule.IMPORT_BULK_WEATHER_DATA_PATH)
    public String importBulk(
            @RequestParam final String areaCode,
            @RequestParam final String dataSource,
            @RequestParam(defaultValue = "csv") final String source,
            @RequestParam(required = false) final String csvContent,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate day,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) final LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) final LocalTime endTime,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticateForControllerCall(session);
            final var report = "open-meteo".equals(source)
                    ? commands.importOpenMeteo(areaCode, dataSource, day, startTime, endTime)
                    : commands.importCsv(areaCode, dataSource, csvContent);
            model.addAttribute("success", true);
            model.addAttribute("report", report);
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", rootMessage(ex));
        }
        return "fragments/weather-import :: result";
    }

    /**
     * The most specific message in the failure chain. The core wraps a reader
     * failure as {@code UncheckedIOException("Could not read weather source", cause)};
     * the cause (e.g. "Open-Meteo returned HTTP 400") is what the operator needs.
     */
    private static String rootMessage(final Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        final String message = root.getMessage();
        return (message == null || message.isBlank()) ? ex.getClass().getSimpleName() : message;
    }

    @GetMapping("/modules/weather-service/weather-data/table")
    public String weatherRows(final Model model) {
        model.addAttribute("weatherData", queries.weatherRows());
        return "fragments/weather-data :: rows";
    }

    @GetMapping("/modules/weather-service/check-area")
    public String checkArea(@RequestParam(defaultValue = "") final String areaCode, final Model model) {
        final String cleanCode = areaCode.trim().toUpperCase();
        String message = "Air control area is registered.";
        boolean valid = true;
        try {
            final AreaCode code = AreaCode.valueOf(cleanCode);
            if (PersistenceContext.repositories().airControlAreas().findByCode(code).isEmpty()) {
                throw new IllegalArgumentException("Air control area is not registered: " + cleanCode);
            }
        } catch (final IllegalArgumentException ex) {
            valid = false;
            message = ex.getMessage();
        }
        model.addAttribute("valid", valid);
        model.addAttribute("message", message);
        return "fragments/form :: validation";
    }

    private boolean canAccessWeather(final HttpSession session) {
        return webSession.hasLogin(session)
                && webSession.canAccessModule(session, AisafeWebModule.WEATHER_SERVICE);
    }

    private String redirectToLoginOrLanding(final HttpSession session) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        return "redirect:" + webSession.landingPath(session);
    }
}
