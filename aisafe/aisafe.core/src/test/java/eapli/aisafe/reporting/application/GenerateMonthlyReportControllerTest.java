package eapli.aisafe.reporting.application;

import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.dto.CompanyFlightStat;
import eapli.aisafe.reporting.dto.MonthlyEvolutionPoint;
import eapli.aisafe.reporting.dto.RouteUsageStat;
import eapli.aisafe.reporting.dto.ViolationAreaStat;
import eapli.aisafe.reporting.infrastructure.HtmlMonthlyReportGenerator;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMonthlyReportControllerTest {

    private final MonthlyStatisticsRepository repo      = new EmptyStatisticsRepository();
    private final HtmlMonthlyReportGenerator  generator = new HtmlMonthlyReportGenerator();

    // ---- constructor guards ----

    @Test
    void constructor_nullRepository_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new GenerateMonthlyReportController(null, generator));
    }

    @Test
    void constructor_nullGenerator_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new GenerateMonthlyReportController(repo, null));
    }

    // ---- generate ----

    @Test
    void generate_validPeriodAndPath_createsHtmlFile(@TempDir Path dir) throws IOException {
        // Arrange
        final var period = new MonthlyReportPeriod(2026, 5);
        final var output = dir.resolve("report.html");
        final var ctrl   = new GenerateMonthlyReportController(repo, generator);

        // Act
        ctrl.generate(period, output);

        // Assert
        assertTrue(Files.exists(output), "report file should exist");
        assertTrue(Files.size(output) > 0, "report file should not be empty");
    }

    @Test
    void generate_htmlContainsExpectedChartIds(@TempDir Path dir) throws IOException {
        // Arrange
        final var period = new MonthlyReportPeriod(2026, 5);
        final var output = dir.resolve("report.html");
        final var ctrl   = new GenerateMonthlyReportController(repo, generator);

        // Act
        ctrl.generate(period, output);
        final String html = Files.readString(output);

        // Assert
        assertAll(
                () -> assertTrue(html.contains("id=\"chartRoutes\""),     "should contain chartRoutes"),
                () -> assertTrue(html.contains("id=\"chartCompanies\""),  "should contain chartCompanies"),
                () -> assertTrue(html.contains("id=\"chartViolations\""), "should contain chartViolations"),
                () -> assertTrue(html.contains("id=\"chartEvolution\""),  "should contain chartEvolution")
        );
    }

    @Test
    void generate_htmlContainsPeriodLabel(@TempDir Path dir) throws IOException {
        // Arrange
        final var period = new MonthlyReportPeriod(2026, 5);
        final var output = dir.resolve("report.html");
        final var ctrl   = new GenerateMonthlyReportController(repo, generator);

        // Act
        ctrl.generate(period, output);
        final String html = Files.readString(output);

        // Assert
        assertTrue(html.contains("May 2026"), "report HTML should contain the period label");
    }

    @Test
    void generate_nullPeriod_throwsNullPointerException(@TempDir Path dir) {
        final var ctrl = new GenerateMonthlyReportController(repo, generator);
        assertThrows(NullPointerException.class,
                () -> ctrl.generate(null, dir.resolve("report.html")));
    }

    @Test
    void generate_nullPath_throwsNullPointerException() {
        final var ctrl = new GenerateMonthlyReportController(repo, generator);
        assertThrows(NullPointerException.class,
                () -> ctrl.generate(new MonthlyReportPeriod(2026, 5), null));
    }

    // ---- preview ----

    @Test
    void preview_validPeriod_returnsDataWithMatchingPeriod() {
        final var period = new MonthlyReportPeriod(2026, 5);
        final var ctrl   = new GenerateMonthlyReportController(repo, generator);

        final var data = ctrl.preview(period);

        assertEquals(period, data.period());
    }

    @Test
    void preview_nullPeriod_throwsNullPointerException() {
        final var ctrl = new GenerateMonthlyReportController(repo, generator);
        assertThrows(NullPointerException.class, () -> ctrl.preview(null));
    }

    // ---- outputFileExtension ----

    @Test
    void outputFileExtension_htmlGenerator_returnsHtml() {
        final var ctrl = new GenerateMonthlyReportController(repo, generator);
        assertEquals("html", ctrl.outputFileExtension());
    }

    /**
     * Minimal fake — verifies controller wiring and HTML generation structure,
     * independent of any real persistence layer.
     */
    private static final class EmptyStatisticsRepository implements MonthlyStatisticsRepository {
        @Override public int countFlights(MonthlyReportPeriod period)     { return 0; }
        @Override public int countViolations(MonthlyReportPeriod period)  { return 0; }
        @Override public int countSimulations(MonthlyReportPeriod period) { return 0; }
        @Override public List<RouteUsageStat> topRoutesByFlights(MonthlyReportPeriod p, int limit) { return List.of(); }
        @Override public List<CompanyFlightStat> topCompaniesByFlights(MonthlyReportPeriod p, int limit) { return List.of(); }
        @Override public List<ViolationAreaStat> violationsByArea(MonthlyReportPeriod p) { return List.of(); }
        @Override public List<MonthlyEvolutionPoint> monthlyEvolution(MonthlyReportPeriod p, int n) { return List.of(); }
    }
}