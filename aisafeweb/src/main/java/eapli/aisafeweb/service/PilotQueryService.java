package eapli.aisafeweb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.Email;

/**
 * Read model for the Pilot Operations workspace: the signed-in pilot's flights
 * with their current plan state (DRAFT / VALIDATED / TESTED). Reads go straight
 * through {@code FlightRepository.flightsOf(Email)} — the same query the core
 * {@code ImportFlightPlanController} uses — so the web list always reflects the
 * persisted flights.
 */
@Service
public class PilotQueryService {

    private static final DateTimeFormatter DEPARTURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** One table row: the flight identity, its aircraft, and its current plan state. */
    public record FlightRow(
            String route,
            String departureDate,
            String departure,
            String aircraft,
            String planStatus,
            int planCount) {
    }

    /** The pilot's flights, newest departure first, optionally filtered per column. */
    public List<FlightRow> flightsOf(final String pilotEmail, final String routeFilter,
            final String statusFilter, final LocalDate dateFilter) {
        return ownFlights(pilotEmail).stream()
                .filter(f -> matchesRoute(f, routeFilter))
                .filter(f -> matchesStatus(f, statusFilter))
                .filter(f -> matchesDate(f, dateFilter))
                .sorted(Comparator.comparing(Flight::departure).reversed())
                .map(PilotQueryService::toRow)
                .toList();
    }

    public List<FlightRow> flightsOf(final String pilotEmail) {
        return flightsOf(pilotEmail, null, null, null);
    }

    /** Flights with DSL content (VALIDATED or TESTED) — the only ones the simulator can run (US121). */
    public List<FlightRow> simulatableFlightsOf(final String pilotEmail) {
        return ownFlights(pilotEmail).stream()
                .filter(f -> f.currentPlan().status() == FlightPlanStatus.VALIDATED
                        || f.currentPlan().status() == FlightPlanStatus.TESTED)
                .sorted(Comparator.comparing(Flight::departure))
                .map(PilotQueryService::toRow)
                .toList();
    }

    /**
     * Flights whose current plan can be tested (US085): only a {@code VALIDATED}
     * plan is testable — the domain promotes it to {@code TESTED} on success and
     * refuses to test a DRAFT or already-TESTED plan.
     */
    public List<FlightRow> testTargetsOf(final String pilotEmail) {
        return ownFlights(pilotEmail).stream()
                .filter(f -> f.currentPlan().status() == FlightPlanStatus.VALIDATED)
                .sorted(Comparator.comparing(Flight::departure))
                .map(PilotQueryService::toRow)
                .toList();
    }

    /**
     * Flights the pilot can attach weather to (US082): any plan that already has
     * DSL content ({@code VALIDATED} or {@code TESTED}). Re-attaching to a TESTED
     * plan demotes it back to VALIDATED.
     */
    public List<FlightRow> weatherTargetsOf(final String pilotEmail) {
        return simulatableFlightsOf(pilotEmail);
    }

    /**
     * Flights a pilot can import a plan into (US121): a {@code DRAFT} plan (first
     * import) or a {@code VALIDATED} one (re-import — supersedes it, demoting the
     * previous plan to DRAFT). TESTED plans are excluded.
     */
    public List<FlightRow> importTargetsOf(final String pilotEmail) {
        return ownFlights(pilotEmail).stream()
                .filter(f -> f.currentPlan().status() == FlightPlanStatus.DRAFT
                        || f.currentPlan().status() == FlightPlanStatus.VALIDATED)
                .sorted(Comparator.comparing(Flight::departure))
                .map(PilotQueryService::toRow)
                .toList();
    }

    public List<String> planStatuses() {
        return List.of(FlightPlanStatus.values()).stream().map(Enum::name).toList();
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    /** One headline metric, matching the backoffice dashboard's KPI card. */
    public record Kpi(String label, String value, String caption, boolean warning) {
    }

    /**
     * The pilot dashboard: four KPIs plus the plan-lifecycle board. The board
     * holds only <em>future</em> flights (departure from now on), grouped by the
     * current plan's state and sorted by departure ascending, so the pilot reads
     * their workflow left-to-right DRAFT → VALIDATED → TESTED.
     */
    public record PilotDashboard(List<Kpi> kpis,
            List<FlightRow> draft, List<FlightRow> validated, List<FlightRow> tested) {
    }

    public PilotDashboard dashboard(final String pilotEmail) {
        final List<Flight> all = ownFlights(pilotEmail);
        final LocalDateTime now = LocalDateTime.now();

        final long drafts = countWithStatus(all, FlightPlanStatus.DRAFT);
        final long validated = countWithStatus(all, FlightPlanStatus.VALIDATED);
        final long dueSoon = all.stream()
                .filter(f -> !f.departure().isBefore(now) && f.departure().isBefore(now.plusHours(24)))
                .filter(f -> f.currentPlan().status() != FlightPlanStatus.TESTED)
                .count();
        final long validatedNoWeather = all.stream()
                .filter(f -> f.currentPlan().status() == FlightPlanStatus.VALIDATED)
                .filter(f -> f.currentPlan().weatherAssignmentsBySegment().isEmpty())
                .count();

        final List<Kpi> kpis = List.of(
                new Kpi("Draft Plans", Long.toString(drafts), "Awaiting a flight plan", false),
                new Kpi("Validated Plans", Long.toString(validated), "Ready to test", false),
                new Kpi("Departing < 24h", Long.toString(dueSoon), "Upcoming, not yet tested", false),
                new Kpi("No Weather", Long.toString(validatedNoWeather),
                        validatedNoWeather == 0 ? "All validated plans have weather data"
                                : "Validated plans missing weather assignment",
                        validatedNoWeather > 0));

        final List<Flight> future = all.stream()
                .filter(f -> !f.departure().isBefore(now))
                .sorted(Comparator.comparing(Flight::departure))
                .toList();

        return new PilotDashboard(kpis,
                column(future, FlightPlanStatus.DRAFT),
                column(future, FlightPlanStatus.VALIDATED),
                column(future, FlightPlanStatus.TESTED));
    }

    private static long countWithStatus(final List<Flight> flights, final FlightPlanStatus status) {
        return flights.stream().filter(f -> f.currentPlan().status() == status).count();
    }

    /** The future flights whose current plan is in the given state, already sorted. */
    private static List<FlightRow> column(final List<Flight> future, final FlightPlanStatus status) {
        return future.stream()
                .filter(f -> f.currentPlan().status() == status)
                .map(PilotQueryService::toRow)
                .toList();
    }

    private List<Flight> ownFlights(final String pilotEmail) {
        final List<Flight> flights = new ArrayList<>();
        if (pilotEmail == null || pilotEmail.isBlank()) {
            return flights;
        }
        PersistenceContext.repositories().flights()
                .flightsOf(Email.valueOf(pilotEmail))
                .forEach(flights::add);
        return flights;
    }

    private static boolean matchesRoute(final Flight flight, final String routeFilter) {
        if (routeFilter == null || routeFilter.isBlank()) {
            return true;
        }
        final String needle = routeFilter.trim().toUpperCase();
        // "flight id" is route + departure date, so match either part
        return flight.route().toString().toUpperCase().contains(needle)
                || flight.identity().toString().toUpperCase().contains(needle);
    }

    private static boolean matchesStatus(final Flight flight, final String statusFilter) {
        return statusFilter == null || statusFilter.isBlank()
                || flight.currentPlan().status().name().equalsIgnoreCase(statusFilter.trim());
    }

    private static boolean matchesDate(final Flight flight, final LocalDate dateFilter) {
        return dateFilter == null || flight.departure().toLocalDate().equals(dateFilter);
    }

    private static FlightRow toRow(final Flight flight) {
        return new FlightRow(
                flight.route().toString(),
                flight.departure().toLocalDate().toString(),
                flight.departure().format(DEPARTURE),
                flight.aircraft().toString(),
                flight.currentPlan().status().name(),
                flight.numberOfPlans());
    }
}