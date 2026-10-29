package eapli.aisafeweb.web;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import eapli.aisafeweb.service.AreaSimRunsService;
import eapli.aisafeweb.service.SimulationReportService;
import jakarta.servlet.http.HttpSession;

/**
 * Flight Control &middot; Simulation Reports (US109 / US111).
 *
 * <p>Lists the persisted {@code SimulationRun}s from the database (left) and offers
 * a form (right) that feeds a finished run folder's {@code report.txt} to the core
 * {@code Generate Simulation Report} use case, which parses and persists it.</p>
 */
@Controller
public class SimulationReportController {

    private static final String GENERATE_PATH = AisafeWebModule.SIMULATION_REPORT_PATH + "/generate";

    private final AisafeWebSession webSession;
    private final SimulationReportService simReports;
    private final AreaSimRunsService areaRuns;

    public SimulationReportController(final AisafeWebSession webSession,
            final SimulationReportService simReports, final AreaSimRunsService areaRuns) {
        this.webSession = webSession;
        this.simReports = simReports;
        this.areaRuns = areaRuns;
    }

    /** Persisted reports list + the generate form's run-folder options. */
    @GetMapping(AisafeWebModule.SIMULATION_REPORT_PATH)
    public String list(final HttpSession session, final Model model) {
        if (!canAccess(session)) {
            return redirectToLoginOrLanding(session);
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL,
                "simulation-report");
        model.addAttribute("runs", simReports.list());
        model.addAttribute("runFolders", areaRuns.list());
        return "simulation-report-list";
    }

    /** One persisted run, rendered in the rich report viewer. */
    @GetMapping(AisafeWebModule.SIMULATION_REPORT_PATH + "/{simId}")
    public String detail(@PathVariable final String simId, final HttpSession session,
            final Model model) {
        if (!canAccess(session)) {
            return redirectToLoginOrLanding(session);
        }
        final var detail = simReports.detail(simId);
        if (detail.isEmpty()) {
            return "redirect:" + AisafeWebModule.SIMULATION_REPORT_PATH;
        }
        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL,
                "simulation-report");
        model.addAttribute("run", detail.get().run());
        model.addAttribute("reportJson", detail.get().reportJson());
        return "simulation-report";
    }

    /** Parses a run folder's report.txt and persists it as a SimulationRun. */
    @PostMapping(GENERATE_PATH)
    public String generate(@RequestParam(required = false) final String runId,
            final HttpSession session, final RedirectAttributes redirect) {
        if (!canAccess(session)) {
            return redirectToLoginOrLanding(session);
        }
        try {
            if (runId == null || runId.isBlank()) {
                throw new IllegalArgumentException("Pick a run folder first.");
            }
            webSession.authenticateForControllerCall(session);
            final SimulationRun saved = simReports.generate(runId);
            redirect.addFlashAttribute("ok", true);
            redirect.addFlashAttribute("message", "Persisted report for " + saved.scenario()
                    + " — " + saved.validationOutcome() + ", " + saved.violations().size()
                    + " violation(s), " + saved.totalFlights() + " flight(s).");
        } catch (final IOException e) {
            redirect.addFlashAttribute("ok", false);
            redirect.addFlashAttribute("message", "Could not read the run folder: " + e.getMessage());
        } catch (final RuntimeException e) {
            redirect.addFlashAttribute("ok", false);
            redirect.addFlashAttribute("message", e.getMessage() == null
                    ? "Could not generate the report." : e.getMessage());
        }
        return "redirect:" + AisafeWebModule.SIMULATION_REPORT_PATH;
    }

    private boolean canAccess(final HttpSession session) {
        return webSession.hasLogin(session)
                && webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL);
    }

    private String redirectToLoginOrLanding(final HttpSession session) {
        if (!webSession.hasLogin(session)) {
            return "redirect:/login";
        }
        return "redirect:" + webSession.landingPath(session);
    }
}