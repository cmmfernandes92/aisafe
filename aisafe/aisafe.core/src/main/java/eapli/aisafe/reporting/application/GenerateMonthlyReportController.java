package eapli.aisafe.reporting.application;

import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.dto.MonthlyReportData;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Application controller for US112 — Generate Monthly Statistics Report.
 * Depends only on interfaces (DIP). Injecting a different generator switches format (OCP).
 */
public class GenerateMonthlyReportController {

    private static final int EVOLUTION_MONTHS = 6;
    private static final int TOP_N = 10;

    private final MonthlyStatisticsRepository repository;
    private final MonthlyReportGenerator      generator;

    public GenerateMonthlyReportController(MonthlyStatisticsRepository repository,
                                           MonthlyReportGenerator generator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.generator  = Objects.requireNonNull(generator,  "generator");
    }

    public void generate(MonthlyReportPeriod period, Path outputPath) throws IOException {
        Objects.requireNonNull(period,     "period");
        Objects.requireNonNull(outputPath, "outputPath");
        generator.generate(collectData(period), outputPath);
    }

    /** Returns the assembled report data without writing any file. Used for text previews. */
    public MonthlyReportData preview(MonthlyReportPeriod period) {
        return collectData(Objects.requireNonNull(period, "period"));
    }

    public String outputFileExtension() {
        return generator.fileExtension();
    }

    private MonthlyReportData collectData(MonthlyReportPeriod period) {
        return new MonthlyReportData(
                period,
                repository.countFlights(period),
                repository.countViolations(period),
                repository.countSimulations(period),
                repository.topRoutesByFlights(period, TOP_N),
                repository.topCompaniesByFlights(period, TOP_N),
                repository.violationsByArea(period),
                repository.monthlyEvolution(period, EVOLUTION_MONTHS)
        );
    }
}
