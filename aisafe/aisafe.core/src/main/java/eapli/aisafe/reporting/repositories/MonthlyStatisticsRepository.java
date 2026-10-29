package eapli.aisafe.reporting.repositories;

import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.dto.CompanyFlightStat;
import eapli.aisafe.reporting.dto.MonthlyEvolutionPoint;
import eapli.aisafe.reporting.dto.RouteUsageStat;
import eapli.aisafe.reporting.dto.ViolationAreaStat;
import java.util.List;

/**
 * Read-only repository for simulation/flight statistics used by the Monthly Report.
 * Returns DTOs (not aggregates) for reporting efficiency (CQRS-lite).
 */
public interface MonthlyStatisticsRepository {
    int countFlights(MonthlyReportPeriod period);
    int countViolations(MonthlyReportPeriod period);
    int countSimulations(MonthlyReportPeriod period);
    List<RouteUsageStat> topRoutesByFlights(MonthlyReportPeriod period, int limit);
    List<CompanyFlightStat> topCompaniesByFlights(MonthlyReportPeriod period, int limit);
    List<ViolationAreaStat> violationsByArea(MonthlyReportPeriod period);
    List<MonthlyEvolutionPoint> monthlyEvolution(MonthlyReportPeriod period, int monthsBack);
}
