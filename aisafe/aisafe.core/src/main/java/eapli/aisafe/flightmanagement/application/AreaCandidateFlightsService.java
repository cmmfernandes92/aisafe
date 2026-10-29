package eapli.aisafe.flightmanagement.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.framework.validations.Preconditions;

/**
 * Finds the flights that are <em>candidates for an area simulation</em>: those a
 * pilot has individually validated (plan in {@link FlightPlanStatus#TESTED}, the
 * US085 readiness gate) whose route crosses a given air control area within a time
 * window. This is the decision input the FCO dashboard surfaces — "these TESTED
 * flights touch your area and could be run together" — and the same selection the
 * future area-simulation egress US will feed to the SCOMP engine, so the two share
 * one crossing predicate ({@link AreaBox}) and cannot drift.
 *
 * <p>Read-only and authz-free by design: it is a dashboard/query building block,
 * like the web read models, guarded at the module boundary. The egress use case
 * that actually <em>runs</em> a simulation enforces FCO authorization itself.</p>
 *
 * <p><b>Window semantics (v1):</b> a flight is in-window when its departure falls
 * within {@code [from, to]}. The doc's precise "area-crossing time within the
 * window" needs per-segment timing and arrives with egress; until then departure
 * is the stated approximation.</p>
 */
public class AreaCandidateFlightsService {

    private final FlightRepository flights;
    private final AirControlAreaRepository areas;
    private final FlightPlanCompiler compiler;

    public AreaCandidateFlightsService(final FlightRepository flights,
            final AirControlAreaRepository areas, final FlightPlanCompiler compiler) {
        Preconditions.noneNull(flights, areas, compiler);
        this.flights = flights;
        this.areas = areas;
        this.compiler = compiler;
    }

    /** One TESTED flight that crosses the area, with how many of its segments are inside. */
    public record CandidateFlight(String route, String departureDate, String departure,
            String aircraft, String designator, int segmentsInArea) {
    }

    /** One TESTED flight plus the parsed plan required to build a native scenario. */
    public record AreaCandidate(Flight flight, ParsedFlightPlan plan, int segmentsInArea) {
        public AreaCandidate {
            Preconditions.noneNull(flight, plan);
            if (segmentsInArea <= 0) {
                throw new IllegalArgumentException("segmentsInArea must be positive");
            }
        }
    }

    /**
     * The TESTED flights crossing {@code areaCode} whose departure falls within
     * {@code [from, to]} (a null bound means unbounded on that side), ordered by
     * departure ascending.
     *
     * @throws IllegalArgumentException if the area code is unknown
     */
    public List<CandidateFlight> candidates(final AreaCode areaCode,
            final LocalDateTime from, final LocalDateTime to) {
        return candidatePlans(areaCode, from, to).stream()
                .map(candidate -> new CandidateFlight(
                        candidate.flight().route().toString(),
                        candidate.flight().departure().toLocalDate().toString(),
                        candidate.flight().departure().toString(),
                        candidate.flight().aircraft().toString(),
                        candidate.plan().designator(),
                        candidate.segmentsInArea()))
                .sorted(Comparator.comparing(CandidateFlight::departure))
                .toList();
    }

    /**
     * Rich form of {@link #candidates(AreaCode, LocalDateTime, LocalDateTime)} for
     * application services that need the actual flight aggregate and recompiled
     * plan, not only display fields.
     *
     * @throws IllegalArgumentException if the area code is unknown
     */
    public List<AreaCandidate> candidatePlans(final AreaCode areaCode,
            final LocalDateTime from, final LocalDateTime to) {
        Preconditions.nonNull(areaCode);
        final AirControlArea area = areas.findByCode(areaCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown air control area: " + areaCode));
        final AreaBox box = AreaBox.of(area);

        final List<AreaCandidate> candidates = new ArrayList<>();
        for (final Flight flight : flights.findAll()) {
            if (flight.currentPlan().status() != FlightPlanStatus.TESTED) {
                continue;
            }
            if (!departureInWindow(flight.departure(), from, to)) {
                continue;
            }
            final ParsedFlightPlan plan = recompile(flight);
            if (plan == null) {
                continue;
            }
            final int inArea = box.segmentsInside(plan);
            if (inArea == 0) {
                continue;
            }
            candidates.add(new AreaCandidate(flight, plan, inArea));
        }
        candidates.sort(Comparator.comparing(candidate -> candidate.flight().departure()));
        return candidates;
    }

    private static boolean departureInWindow(final LocalDateTime departure,
            final LocalDateTime from, final LocalDateTime to) {
        return (from == null || !departure.isBefore(from))
                && (to == null || !departure.isAfter(to));
    }

    /** Recompiles the flight's current plan DSL, or null if it has none / won't compile. */
    private ParsedFlightPlan recompile(final Flight flight) {
        final String dsl = flight.currentPlan().rawDslContent();
        if (dsl == null || dsl.isBlank()) {
            return null;
        }
        final FlightPlanCompilationResult result = compiler.compile(dsl);
        if (!result.ok() || result.plan().isEmpty()) {
            return null;
        }
        return result.plan().get();
    }
}
