package eapli.aisafe.app.fco.console.presentation.reporting;

import eapli.aisafe.reporting.application.GenerateMonthlyReportController;
import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.dto.MonthlyReportData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

public class GenerateMonthlyReportUI extends AbstractUI {

    private static final String LINE  = "  " + "─".repeat(55);
    private static final String THICK = "  " + "═".repeat(55);

    private final GenerateMonthlyReportController controller;

    public GenerateMonthlyReportUI(GenerateMonthlyReportController controller) {
        this.controller = controller;
    }

    @Override
    public String headline() {
        return "Generate Monthly Statistics Report";
    }

    @Override
    protected boolean doShow() {
        final int currentYear  = LocalDate.now().getYear();
        final int currentMonth = LocalDate.now().getMonthValue();

        int year  = readInt("Year  (2000-2099) [" + currentYear  + "]: ", currentYear,  2000, 2099);
        int month = readInt("Month (1-12)      [" + currentMonth + "]: ", currentMonth, 1,    12);

        MonthlyReportPeriod period;
        try {
            period = new MonthlyReportPeriod(year, month);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid period: " + e.getMessage());
            return false;
        }

        // ── text preview in console ─────────────────────────────────
        final MonthlyReportData data = controller.preview(period);
        printTextReport(data);

        // ── save HTML to docs/sprint-3/us112/ ───────────────────────
        final Path docsPath = docsReportPath(year, month);
        final String defaultName = String.format("report-%04d-%02d.%s",
                year, month, controller.outputFileExtension());
        final Path defaultPath = Path.of(defaultName).toAbsolutePath();

        System.out.println();
        System.out.println("Output will be saved to (press Enter to accept):");
        System.out.println("  " + defaultPath);
        final String rawPath = Console.readLine("Or enter a different path: ");
        final Path outputPath = rawPath.isBlank() ? defaultPath : Path.of(rawPath.trim()).toAbsolutePath();

        try {
            controller.generate(period, outputPath);
            System.out.println("✓ HTML report: " + outputPath);

            // also copy to docs
            controller.generate(period, docsPath);
            System.out.println("✓ Saved to docs: " + docsPath);
            System.out.println("  Open the HTML file in a browser to view the charts.");
        } catch (IOException e) {
            System.out.println("Error writing report: " + e.getMessage());
        }
        return false;
    }

    // ── text report printer ─────────────────────────────────────────

    private void printTextReport(MonthlyReportData d) {
        System.out.println();
        System.out.println(THICK);
        System.out.printf("  AISafe Monthly Statistics — %s%n", d.period().label());
        System.out.println(THICK);
        System.out.printf("  Flights: %-6d   Violations: %-4d   Simulations: %d%n",
                d.totalFlights(), d.totalViolations(), d.totalSimulations());

        System.out.println(LINE);
        System.out.println("  Most-Used Routes");
        System.out.println(LINE);
        for (var r : d.topRoutes()) {
            System.out.printf("  %-24s %s %d%n",
                    r.routeName() + " (" + r.companyName() + ")",
                    bar(r.flightCount(), 40), r.flightCount());
        }

        System.out.println(LINE);
        System.out.println("  Companies with Most Flights");
        System.out.println(LINE);
        for (var c : d.topCompanies()) {
            System.out.printf("  %-28s %d flights%n",
                    c.companyName() + " (" + c.companyIata() + ")", c.flightCount());
        }

        System.out.println(LINE);
        System.out.println("  Safety Violations by Area");
        System.out.println(LINE);
        for (var a : d.violationsByArea()) {
            System.out.printf("  %-30s %d violation%s%n",
                    a.areaName() + " (" + a.areaCode() + ")",
                    a.violationCount(), a.violationCount() == 1 ? "" : "s");
        }

        System.out.println(LINE);
        System.out.println("  Monthly Evolution (flights / violations)");
        System.out.println(LINE);
        for (var e : d.evolution()) {
            System.out.printf("  %-14s  %3d flights   %d violations%n",
                    e.label(), e.totalFlights(), e.totalViolations());
        }
        System.out.println(THICK);
    }

    /** Builds a small ASCII bar proportional to value (max 20 chars). */
    private static String bar(int value, int max) {
        final int len = Math.max(1, Math.round(value * 20f / max));
        return "█".repeat(len);
    }

    /** Path to docs/sprint-3/us112/ relative to the project root. */
    private static Path docsReportPath(int year, int month) {
        // When running from any module under aisafe/, walk up to the project root
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !root.resolve("docs").toFile().isDirectory()) {
            root = root.getParent();
        }
        final Path docsDir = (root != null ? root : Path.of("."))
                .resolve("docs/sprint-3/us112");
        return docsDir.resolve(String.format("report-%04d-%02d.html", year, month));
    }

    private int readInt(String prompt, int defaultValue, int min, int max) {
        while (true) {
            final String raw = Console.readLine(prompt);
            if (raw.isBlank()) return defaultValue;
            try {
                final int v = Integer.parseInt(raw.trim());
                if (v >= min && v <= max) return v;
                System.out.printf("  Value must be between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid integer.");
            }
        }
    }
}
