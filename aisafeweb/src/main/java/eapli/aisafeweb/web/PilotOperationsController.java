package eapli.aisafeweb.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightplanmanagement.dsl.CompilationResult;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporter;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporterFactory;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanDTO;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.FlightCommandService;
import eapli.aisafeweb.service.FlightPlanTestCommandService;
import eapli.aisafeweb.service.FlightWeatherCommandService;
import eapli.aisafeweb.service.PilotQueryService;
import eapli.aisafeweb.service.WeatherConsultService;
import jakarta.servlet.http.HttpSession;

@Controller
public class PilotOperationsController {

    /** HTMX endpoint refreshing the shared "My Flights" table with column filters. */
    static final String FLIGHTS_TABLE_PATH = "/modules/pilot-operations/flights/table";
    private static final String WEATHER_RESULTS_PATH =
            AisafeWebModule.PILOT_CONSULT_WEATHER_DATA_PATH + "/results";

    private final AisafeWebSession webSession;
    private final FlightCommandService flightCommands;
    private final FlightWeatherCommandService weatherCommands;
    private final FlightPlanTestCommandService testCommands;
    private final PilotQueryService pilotQueries;
    private final WeatherConsultService weatherConsult;

    public PilotOperationsController(final AisafeWebSession webSession,
            final FlightCommandService flightCommands,
            final FlightWeatherCommandService weatherCommands,
            final FlightPlanTestCommandService testCommands,
            final PilotQueryService pilotQueries,
            final WeatherConsultService weatherConsult) {
        this.webSession = webSession;
        this.flightCommands = flightCommands;
        this.weatherCommands = weatherCommands;
        this.testCommands = testCommands;
        this.pilotQueries = pilotQueries;
        this.weatherConsult = weatherConsult;
    }

    // ── Pilot dashboard ──────────────────────────────────────────────────────

    // Literal path wins over ModuleController's "/modules/{moduleId}", so the
    // pilot lands here instead of the generic module placeholder.
    @GetMapping("/modules/pilot-operations")
    public String dashboard(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "dashboard");
        if (guard != null) {
            return guard;
        }
        model.addAttribute("dashboard", pilotQueries.dashboard(webSession.userEmail(session)));
        return "pilot-dashboard";
    }

    // ── Create Flight (US080) ────────────────────────────────────────────────

    @GetMapping(AisafeWebModule.CREATE_FLIGHT_PATH)
    public String createFlight(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "create-flight");
        if (guard != null) {
            return guard;
        }
        return "pilot-create-flight";
    }

    @PostMapping(AisafeWebModule.CREATE_FLIGHT_PATH)
    public String createFlight(
            @RequestParam final String routeName,
            @RequestParam final String registration,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime departure,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticatePilotForControllerCall(session);
            final Flight flight = flightCommands.createDraft(routeName, departure, registration);
            model.addAttribute("success", true);
            model.addAttribute("message", "Flight created for " + flight.route() + " on "
                    + flight.identity().departureDate() + " — initial plan is "
                    + flight.currentPlan().status() + ".");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    // ── My Flights table (shared across Pilot Operations pages) ─────────────

    @GetMapping(FLIGHTS_TABLE_PATH)
    public String flightsTable(
            @RequestParam(required = false) final String route,
            @RequestParam(required = false) final String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date,
            final HttpSession session,
            final Model model) {
        model.addAttribute("flights",
                pilotQueries.flightsOf(webSession.userEmail(session), route, status, date));
        return "fragments/pilot-flights :: rows";
    }

    // ── Import Flight Plan (US121) ───────────────────────────────────────────

    @GetMapping(AisafeWebModule.IMPORT_FLIGHT_PLAN_PATH)
    public String importFlightPlan(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "import-flight-plan");
        if (guard != null) {
            return guard;
        }
        model.addAttribute("importFlights", pilotQueries.importTargetsOf(webSession.userEmail(session)));
        model.addAttribute("supportedFormats", supportedFormats());
        return "pilot-import-flight-plan";
    }

    @PostMapping(AisafeWebModule.IMPORT_FLIGHT_PLAN_PATH)
    public String importFlightPlan(
            @RequestParam final String flightKey,
            @RequestParam(defaultValue = "plan.flight") final String fileName,
            @RequestParam final String dslContent,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticatePilotForControllerCall(session);

            // Adapter-side US120 validation (lexical/syntactic/semantic), exactly
            // like the pilot console: the core controller only sees valid content.
            final String safeName = fileName == null || fileName.isBlank() ? "plan.flight" : fileName.trim();
            final FlightPlanImporter importer = FlightPlanImporterFactory.forPath(Path.of(safeName))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unsupported file format. Supported extensions: " + supportedFormats()));

            final CompilationResult result;
            try (InputStream in = new ByteArrayInputStream(dslContent.getBytes(StandardCharsets.UTF_8))) {
                result = importer.importFrom(in, safeName);
            }

            if (!result.ok()) {
                model.addAttribute("success", false);
                model.addAttribute("errors", result.errors());
                model.addAttribute("message", "Validation failed — " + result.errors().size() + " error(s):");
                return "fragments/flight-plan-import :: result";
            }

            final List<FlightPlanDTO> plans = result.file().orElseThrow().flightPlans();
            if (plans.size() != 1) {
                throw new IllegalArgumentException("This file describes " + plans.size()
                        + " flight plans; import one plan per file into the selected flight.");
            }

            // flightKey = "<route>|<ISO departure date>" — the flight's identity (US080)
            final String[] key = flightKey.split("\\|", 2);
            if (key.length != 2) {
                throw new IllegalArgumentException("Select a target flight first.");
            }
            final Flight flight = flightCommands.importPlan(key[0], LocalDate.parse(key[1]), dslContent);

            model.addAttribute("success", true);
            model.addAttribute("message", "Plan " + plans.get(0).designator().value()
                    + " imported into flight " + flight.route() + " on "
                    + flight.identity().departureDate() + " — plan is now "
                    + flight.currentPlan().status() + ".");
        } catch (final IOException | RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/flight-plan-import :: result";
    }

    // ── Add Weather Data (US082) ─────────────────────────────────────────────

    @GetMapping(AisafeWebModule.ADD_WEATHER_DATA_PATH)
    public String addWeatherData(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "add-weather-data");
        if (guard != null) {
            return guard;
        }
        model.addAttribute("weatherFlights", pilotQueries.weatherTargetsOf(webSession.userEmail(session)));
        return "pilot-add-weather";
    }

    /** HTMX fragment: the per-segment forecast form for the picked flight. */
    @GetMapping(AisafeWebModule.ADD_WEATHER_DATA_PATH + "/segments")
    public String addWeatherSegments(
            @RequestParam(required = false) final String flightKey,
            @RequestParam(required = false) final String snapshotKey,
            final HttpSession session,
            final Model model) {
        final String[] key = splitFlightKey(flightKey);
        if (key == null) {
            model.addAttribute("plan", FlightWeatherCommandService.PlanWeatherView
                    .failed(flightKey, List.of("Select a flight first.")));
            return "fragments/weather-segments :: form";
        }
        model.addAttribute("plan", weatherCommands.planWeather(
                webSession.userEmail(session), key[0], LocalDate.parse(key[1]), snapshotKey));
        return "fragments/weather-segments :: form";
    }

    @PostMapping(AisafeWebModule.ADD_WEATHER_DATA_PATH)
    public String addWeatherData(
            @RequestParam final String flightKey,
            @RequestParam final Map<String, String> allParams,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticatePilotForControllerCall(session);
            final String[] key = splitFlightKey(flightKey);
            if (key == null) {
                throw new IllegalArgumentException("Select a target flight first.");
            }
            final int attached = weatherCommands.attach(key[0], LocalDate.parse(key[1]), forecastSelections(allParams));
            model.addAttribute("success", true);
            model.addAttribute("message", "Weather attached to " + attached + " segment(s)"
                    + " — a TESTED plan is demoted to VALIDATED, so re-test after changing weather.");
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    /** Collects {@code forecast_<segmentIndex> -> forecastId} form fields into an index map. */
    private static Map<Integer, String> forecastSelections(final Map<String, String> allParams) {
        final Map<Integer, String> selections = new LinkedHashMap<>();
        for (final Map.Entry<String, String> e : allParams.entrySet()) {
            if (e.getKey().startsWith("forecast_")) {
                try {
                    selections.put(Integer.parseInt(e.getKey().substring("forecast_".length())), e.getValue());
                } catch (final NumberFormatException ignored) {
                    // skip malformed field names
                }
            }
        }
        return selections;
    }

    // ── Test Flight Plan (US085) ─────────────────────────────────────────────

    @GetMapping(AisafeWebModule.TEST_FLIGHT_PLAN_PATH)
    public String testFlightPlan(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "test-flight-plan");
        if (guard != null) {
            return guard;
        }
        model.addAttribute("testFlights", pilotQueries.testTargetsOf(webSession.userEmail(session)));
        return "pilot-test-flight";
    }

    @PostMapping(AisafeWebModule.TEST_FLIGHT_PLAN_PATH)
    public String testFlightPlan(
            @RequestParam final String flightKey,
            final HttpSession session,
            final Model model) {
        try {
            webSession.authenticatePilotForControllerCall(session);
            final String[] key = splitFlightKey(flightKey);
            if (key == null) {
                throw new IllegalArgumentException("Select a flight to test first.");
            }
            final FlightPlanTestCommandService.TestOutcome outcome =
                    testCommands.test(key[0], LocalDate.parse(key[1]));
            model.addAttribute("success", outcome.success());
            model.addAttribute("message", outcome.success()
                    ? "Plan passed — promoted to TESTED. " + outcome.message()
                    : "Plan did not pass (state unchanged): " + outcome.message());
        } catch (final RuntimeException ex) {
            model.addAttribute("success", false);
            model.addAttribute("message", ex.getMessage());
        }
        return "fragments/form :: result";
    }

    // ── Consult Weather Data (US043) ────────────────────────────────────────

    @GetMapping(AisafeWebModule.PILOT_CONSULT_WEATHER_DATA_PATH)
    public String consultWeatherData(final HttpSession session, final Model model) {
        final String guard = guardPilotPage(session, model, "consult-weather-data");
        if (guard != null) {
            return guard;
        }
        webSession.authenticateWeatherConsultForControllerCall(session);
        model.addAttribute("areas", weatherConsult.areas());
        model.addAttribute("resultsPath", WEATHER_RESULTS_PATH);
        return "pilot-consult-weather";
    }

    @GetMapping(WEATHER_RESULTS_PATH)
    public String consultWeatherDataResults(
            @RequestParam(required = false) final String areaCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate day,
            final HttpSession session,
            final Model model) {
        final String guard = guardPilotPage(session, model, "consult-weather-data");
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

    /** {@code "<route>|<ISO date>"} → {@code [route, date]}, or null if absent/malformed. */
    private static String[] splitFlightKey(final String flightKey) {
        if (flightKey == null || flightKey.isBlank()) {
            return null;
        }
        final String[] parts = flightKey.split("\\|", 2);
        return parts.length == 2 ? parts : null;
    }

    /**
     * Session/module guard shared by all Pilot Operations pages. Returns a
     * redirect when access is denied; otherwise adds the shell attributes plus
     * the pilot's flights (the shared "My Flights" panel) and returns null.
     */
    private String guardPilotPage(final HttpSession session, final Model model, final String activePage) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        if (!webSession.canAccessModule(session, AisafeWebModule.PILOT_OPERATIONS)) {
            return "redirect:" + webSession.landingPath(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.PILOT_OPERATIONS, activePage);
        model.addAttribute("flights", pilotQueries.flightsOf(webSession.userEmail(session)));
        model.addAttribute("planStatuses", pilotQueries.planStatuses());
        return null;
    }

    private static String supportedFormats() {
        return FlightPlanImporterFactory.supportedExtensions().stream()
                .map(e -> "*." + e)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
