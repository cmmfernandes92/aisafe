package eapli.aisafe.reporting.infrastructure;

import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.reporting.domain.FlightExecutionStatus;
import eapli.aisafe.reporting.domain.MonthlyReportPeriod;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.dto.CompanyFlightStat;
import eapli.aisafe.reporting.dto.MonthlyEvolutionPoint;
import eapli.aisafe.reporting.dto.RouteUsageStat;
import eapli.aisafe.reporting.dto.ViolationAreaStat;
import eapli.aisafe.reporting.repositories.MonthlyStatisticsRepository;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;

/**
 * MonthlyStatisticsRepository backed by persisted {@link SimulationRun} aggregates.
 *
 * <p>CQRS-lite: aggregates the SimulationRun stream returned by
 * {@link SimulationRunRepository#findInPeriod} into the reporting DTOs that
 * US112 consumes. Route → company is resolved through {@link FlightRouteRepository}
 * (flight refs are formatted as {@code ROUTE@yyyy-MM-dd}); area code → name
 * through {@link AirControlAreaRepository}.
 */
public class AggregatedMonthlyStatisticsRepository implements MonthlyStatisticsRepository {

    private final SimulationRunRepository runs;
    private final FlightRouteRepository routes;
    private final AirControlAreaRepository areas;

    public AggregatedMonthlyStatisticsRepository(final SimulationRunRepository runs,
            final FlightRouteRepository routes, final AirControlAreaRepository areas) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.routes = Objects.requireNonNull(routes, "routes");
        this.areas = Objects.requireNonNull(areas, "areas");
    }

    @Override
    public int countFlights(final MonthlyReportPeriod period) {
        return totalsFor(period).flights;
    }

    @Override
    public int countViolations(final MonthlyReportPeriod period) {
        return totalsFor(period).violations;
    }

    @Override
    public int countSimulations(final MonthlyReportPeriod period) {
        int count = 0;
        for (final SimulationRun ignored : runsIn(period)) {
            count++;
        }
        return count;
    }

    @Override
    public List<RouteUsageStat> topRoutesByFlights(final MonthlyReportPeriod period, final int limit) {
        final Map<String, Integer> counts = new HashMap<>();
        for (final SimulationRun run : runsIn(period)) {
            for (final FlightExecutionStatus status : run.flightStatuses()) {
                final String routeName = parseRouteName(status.flightRef());
                if (routeName != null) {
                    counts.merge(routeName, 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new RouteUsageStat(e.getKey(), companyNameFor(e.getKey()), e.getValue()))
                .toList();
    }

    @Override
    public List<CompanyFlightStat> topCompaniesByFlights(final MonthlyReportPeriod period, final int limit) {
        final Map<String, Integer> countsByIata = new HashMap<>();
        final Map<String, String> namesByIata = new HashMap<>();
        for (final SimulationRun run : runsIn(period)) {
            for (final FlightExecutionStatus status : run.flightStatuses()) {
                final String routeName = parseRouteName(status.flightRef());
                if (routeName == null) {
                    continue;
                }
                final Optional<FlightRoute> route = routeOf(routeName);
                if (route.isEmpty()) {
                    continue;
                }
                final String iata = route.get().company().iataCode().value();
                countsByIata.merge(iata, 1, Integer::sum);
                namesByIata.putIfAbsent(iata, route.get().company().companyName());
            }
        }
        return countsByIata.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new CompanyFlightStat(e.getKey(),
                        namesByIata.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .toList();
    }

    @Override
    public List<ViolationAreaStat> violationsByArea(final MonthlyReportPeriod period) {
        final Map<AreaCode, Integer> counts = new HashMap<>();
        for (final SimulationRun run : runsIn(period)) {
            final int v = run.violations().size();
            if (v == 0) {
                continue;
            }
            counts.merge(run.areaCode(), v, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<AreaCode, Integer>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().value(), Comparator.naturalOrder()))
                .map(e -> new ViolationAreaStat(e.getKey().value(), areaNameFor(e.getKey()), e.getValue()))
                .toList();
    }

    @Override
    public List<MonthlyEvolutionPoint> monthlyEvolution(final MonthlyReportPeriod period, final int monthsBack) {
        final Deque<MonthlyReportPeriod> stack = new ArrayDeque<>(monthsBack);
        MonthlyReportPeriod cursor = period;
        for (int i = 0; i < monthsBack; i++) {
            stack.push(cursor);
            cursor = cursor.previous();
        }
        final List<MonthlyEvolutionPoint> ordered = new ArrayList<>(monthsBack);
        while (!stack.isEmpty()) {
            final MonthlyReportPeriod p = stack.pop();
            final MonthlyTotals totals = totalsFor(p);
            ordered.add(new MonthlyEvolutionPoint(p.label(), totals.flights, totals.violations));
        }
        return ordered;
    }

    private Iterable<SimulationRun> runsIn(final MonthlyReportPeriod period) {
        final YearMonth ym = period.toYearMonth();
        return runs.findInPeriod(
                ym.atDay(1).atStartOfDay(),
                ym.plusMonths(1).atDay(1).atStartOfDay());
    }

    private MonthlyTotals totalsFor(final MonthlyReportPeriod period) {
        int flights = 0;
        int violations = 0;
        for (final SimulationRun run : runsIn(period)) {
            flights += run.totalFlights();
            violations += run.violations().size();
        }
        return new MonthlyTotals(flights, violations);
    }

    /** Flight refs are formatted {@code ROUTE@yyyy-MM-dd} (US111 sidecar). */
    private static String parseRouteName(final String flightRef) {
        if (flightRef == null || flightRef.isEmpty() || flightRef.charAt(0) == '#') {
            return null;
        }
        final int at = flightRef.indexOf('@');
        return at < 0 ? flightRef : flightRef.substring(0, at);
    }

    private Optional<FlightRoute> routeOf(final String routeName) {
        try {
            return routes.ofIdentity(RouteName.valueOf(routeName));
        } catch (final IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String companyNameFor(final String routeName) {
        return routeOf(routeName).map(r -> r.company().companyName()).orElse("");
    }

    private String areaNameFor(final AreaCode code) {
        return areas.findByCode(code).map(AirControlArea::name).map(n -> n.value()).orElse(code.value());
    }

    private static final class MonthlyTotals {
        final int flights;
        final int violations;
        MonthlyTotals(final int flights, final int violations) {
            this.flights = flights;
            this.violations = violations;
        }
    }
}