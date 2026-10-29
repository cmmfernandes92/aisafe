package eapli.aisafeweb.web;

import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.application.GenerateMonthlyReportController;
import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.infrastructure.HtmlMonthlyReportGenerator;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;
import eapli.aisafeweb.security.AisafeWebModule;
import eapli.aisafeweb.security.AisafeWebSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class MonthlyReportController {

    private static final MonthlyStatisticsRepository REPO =
            PersistenceContext.repositories().monthlyStatistics();

    private static final int EVOLUTION_MONTHS = 6;
    private static final int TOP_N = 10;

    private static final Map<Integer, String> MONTH_NAMES;
    static {
        MONTH_NAMES = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            MONTH_NAMES.put(i, Month.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
    }

    private static final List<String> PALETTE = List.of(
            "#3b82f6","#10b981","#f59e0b","#ef4444","#8b5cf6",
            "#06b6d4","#f97316","#84cc16","#ec4899","#14b8a6");

    private final AisafeWebSession webSession;

    public MonthlyReportController(final AisafeWebSession webSession) {
        this.webSession = webSession;
    }

    @GetMapping(AisafeWebModule.MONTHLY_REPORT_PATH)
    public String monthlyReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            final HttpSession session,
            final Model model) {

        if (!canAccess(session)) {
            return redirectToLoginOrLanding(session);
        }

        final int y = year  != null ? year  : LocalDate.now().getYear();
        final int m = month != null ? month : LocalDate.now().getMonthValue();

        final MonthlyReportPeriod period;
        try {
            period = new MonthlyReportPeriod(y, m);
        } catch (IllegalArgumentException e) {
            return "redirect:" + AisafeWebModule.MONTHLY_REPORT_PATH;
        }

        webSession.addShellAttributes(model, session, AisafeWebModule.FLIGHT_CONTROL, "monthly-report");

        model.addAttribute("monthNames", MONTH_NAMES);

        // period
        model.addAttribute("year",        y);
        model.addAttribute("month",       m);
        model.addAttribute("periodLabel", period.label());

        // KPIs
        model.addAttribute("totalFlights",     REPO.countFlights(period));
        model.addAttribute("totalViolations",  REPO.countViolations(period));
        model.addAttribute("totalSimulations", REPO.countSimulations(period));

        // Chart 1 — Routes
        final var routes = REPO.topRoutesByFlights(period, TOP_N);
        model.addAttribute("routeLabels", routes.stream()
                .map(r -> r.routeName() + " (" + r.companyName() + ")").toList());
        model.addAttribute("routeData",   routes.stream().map(r -> r.flightCount()).toList());
        model.addAttribute("routeColors", paletteSlice(routes.size()));
        model.addAttribute("routeCount",  routes.size());

        // Chart 2 — Companies
        final var companies = REPO.topCompaniesByFlights(period, TOP_N);
        model.addAttribute("companyLabels", companies.stream()
                .map(c -> c.companyName() + " (" + c.companyIata() + ")").toList());
        model.addAttribute("companyData",   companies.stream().map(c -> c.flightCount()).toList());
        model.addAttribute("companyColors", paletteSlice(companies.size()));
        model.addAttribute("companyCount",  companies.size());

        // Chart 3 — Violations by area
        final var areas = REPO.violationsByArea(period);
        model.addAttribute("areaLabels", areas.stream()
                .map(a -> a.areaName() + " (" + a.areaCode() + ")").toList());
        model.addAttribute("areaData",   areas.stream().map(a -> a.violationCount()).toList());

        // Chart 4 — Monthly evolution
        final var evolution = REPO.monthlyEvolution(period, EVOLUTION_MONTHS);
        model.addAttribute("evolutionLabels",     evolution.stream().map(e -> e.label()).toList());
        model.addAttribute("evolutionFlights",    evolution.stream().map(e -> e.totalFlights()).toList());
        model.addAttribute("evolutionViolations", evolution.stream().map(e -> e.totalViolations()).toList());

        // flash message from save action
        return "monthly-report";
    }

    /** POST: save HTML to docs/sprint-3/us112/ and redirect back with flash message. */
    @PostMapping(AisafeWebModule.MONTHLY_REPORT_PATH + "/save-to-docs")
    public String saveToDocs(
            @RequestParam int year,
            @RequestParam int month,
            final HttpSession session,
            final RedirectAttributes flash) {

        if (!canAccess(session)) return redirectToLoginOrLanding(session);

        try {
            final var period = new MonthlyReportPeriod(year, month);
            final Path dest  = docsPath(year, month);
            final var ctrl   = new GenerateMonthlyReportController(REPO, new HtmlMonthlyReportGenerator());
            ctrl.generate(period, dest);
            flash.addFlashAttribute("saveSuccess", "Report saved to docs: " + dest.toAbsolutePath());
        } catch (IllegalArgumentException | IOException e) {
            flash.addFlashAttribute("saveError", "Save failed: " + e.getMessage());
        }
        return "redirect:" + AisafeWebModule.MONTHLY_REPORT_PATH + "?year=" + year + "&month=" + month;
    }

    /** GET: serve the HTML file as a browser download. */
    @GetMapping(AisafeWebModule.MONTHLY_REPORT_PATH + "/download")
    public ResponseEntity<byte[]> download(
            @RequestParam int year,
            @RequestParam int month,
            final HttpSession session) throws IOException {

        if (!canAccess(session)) return ResponseEntity.status(302).build();

        final var period   = new MonthlyReportPeriod(year, month);
        final var ctrl     = new GenerateMonthlyReportController(REPO, new HtmlMonthlyReportGenerator());
        final Path tmpFile = Files.createTempFile("aisafe-report-", ".html");
        try {
            ctrl.generate(period, tmpFile);
            final byte[] bytes = Files.readAllBytes(tmpFile);
            final String filename = String.format("report-%04d-%02d.html", year, month);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.TEXT_HTML)
                    .body(bytes);
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    /** Resolves docs/sprint-3/us112/ by walking up from the working directory. */
    private static Path docsPath(int year, int month) {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !root.resolve("docs").toFile().isDirectory()) {
            root = root.getParent();
        }
        final Path dir = (root != null ? root : Path.of(".")).resolve("docs/sprint-3/us112");
        return dir.resolve(String.format("report-%04d-%02d.html", year, month));
    }

    private List<String> paletteSlice(int n) {
        final var result = new java.util.ArrayList<String>(n);
        for (int i = 0; i < n; i++) result.add(PALETTE.get(i % PALETTE.size()));
        return result;
    }

    private boolean canAccess(final HttpSession session) {
        return webSession.hasLogin(session)
                && webSession.canAccessModule(session, AisafeWebModule.FLIGHT_CONTROL);
    }

    private String redirectToLoginOrLanding(final HttpSession session) {
        if (!webSession.hasLogin(session)) return "redirect:/login";
        return "redirect:" + webSession.landingPath(session);
    }
}
