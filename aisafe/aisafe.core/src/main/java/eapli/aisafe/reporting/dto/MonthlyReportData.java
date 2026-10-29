package eapli.aisafe.reporting.dto;

import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import java.util.List;
import java.util.Objects;

public record MonthlyReportData(
        MonthlyReportPeriod         period,
        int                         totalFlights,
        int                         totalViolations,
        int                         totalSimulations,
        List<RouteUsageStat>        topRoutes,
        List<CompanyFlightStat>     topCompanies,
        List<ViolationAreaStat>     violationsByArea,
        List<MonthlyEvolutionPoint> evolution
) {
    public MonthlyReportData {
        Objects.requireNonNull(period,          "period");
        Objects.requireNonNull(topRoutes,        "topRoutes");
        Objects.requireNonNull(topCompanies,     "topCompanies");
        Objects.requireNonNull(violationsByArea, "violationsByArea");
        Objects.requireNonNull(evolution,        "evolution");
    }
}
