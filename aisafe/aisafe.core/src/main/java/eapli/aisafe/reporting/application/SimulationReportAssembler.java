package eapli.aisafe.reporting.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.reporting.application.parsing.ParsedFlightStatus;
import eapli.aisafe.reporting.application.parsing.ParsedSimulationReport;
import eapli.aisafe.reporting.application.parsing.ParsedViolation;
import eapli.aisafe.reporting.domain.FlightExecutionStatus;
import eapli.aisafe.reporting.domain.FlightOutcome;
import eapli.aisafe.reporting.domain.InvolvedFlight;
import eapli.aisafe.reporting.domain.SafetyViolation;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.ValidationOutcome;
import eapli.aisafe.reporting.domain.ViolationKind;
import eapli.framework.validations.Preconditions;

/**
 * Domain Service / Factory (US111 §4.1): turns a {@link ParsedSimulationReport}
 * plus the FCO's resolved {@link AreaCode} into a constructed {@link SimulationRun}.
 * The assembly spans more than one aggregate ({@code SimulationRun} + {@code Flight}
 * + the area), so it belongs to no single aggregate. It never persists.
 *
 * <p>Each report flight is resolved to its domain {@link FlightId} by identity
 * (route + departure date) via the {@code flights.csv} sidecar; the
 * {@link FlightRepository} is consulted to confirm/normalise the identity.
 */
public class SimulationReportAssembler {

    private final FlightRepository flights;

    public SimulationReportAssembler(final FlightRepository flights) {
        Preconditions.nonNull(flights);
        this.flights = flights;
    }

    public SimulationRun assemble(final ParsedSimulationReport report, final AreaCode area) {
        Preconditions.noneNull(report, area);

        final List<FlightExecutionStatus> statuses = new ArrayList<>();
        for (final ParsedFlightStatus s : report.statuses()) {
            statuses.add(new FlightExecutionStatus(
                    s.flightId(),
                    resolveRef(s.identity(), s.flightId()),
                    FlightOutcome.fromReport(s.outcome(), s.exitCode()),
                    s.exitCode()));
        }

        final List<SafetyViolation> violations = new ArrayList<>();
        for (final ParsedViolation v : report.violations()) {
            final ViolationKind kind = ViolationKind.fromReport(v.kind());
            if (kind.isAltitude()) {
                // single-flight altitude bust — decode the C self-paired row back to ONE flight
                final InvolvedFlight a = new InvolvedFlight(v.flight1(),
                        resolveRef(v.identity1(), v.flight1()),
                        v.x1(), v.y1(), v.alt1(), v.vx1(), v.vy1(), v.vz1());
                violations.add(SafetyViolation.altitude(
                        v.step(),
                        timestampOf(report.runTimestamp(), v.step(), report.secondsPerStep()),
                        kind, a, v.verticalDistance()));
            } else {
                final InvolvedFlight a = new InvolvedFlight(v.flight1(),
                        resolveRef(v.identity1(), v.flight1()),
                        v.x1(), v.y1(), v.alt1(), v.vx1(), v.vy1(), v.vz1());
                final InvolvedFlight b = new InvolvedFlight(v.flight2(),
                        resolveRef(v.identity2(), v.flight2()),
                        v.x2(), v.y2(), v.alt2(), v.vx2(), v.vy2(), v.vz2());
                violations.add(SafetyViolation.separation(
                        v.step(),
                        timestampOf(report.runTimestamp(), v.step(), report.secondsPerStep()),
                        a, b, v.horizontalDistance(), v.verticalDistance()));
            }
        }

        return new SimulationRun(
                area,
                report.runTimestamp(),
                report.scenario(),
                report.totalFlights(),
                report.secondsPerStep(),
                report.endReason(),
                ValidationOutcome.fromReport(report.validation()),
                statuses,
                violations);
    }

    /** Derives the wall-clock timestamp: {@code run_timestamp + step × seconds_per_step} (US111.3). */
    private static LocalDateTime timestampOf(final LocalDateTime runTimestamp, final int step,
            final double secondsPerStep) {
        return runTimestamp.plusSeconds(Math.round(step * secondsPerStep));
    }

    /**
     * The domain reference stored for a report flight. When the sidecar resolved
     * an identity, the repository is asked to normalise it (US112 enrichment hook);
     * absent any mapping the surrogate id is kept as {@code #id}.
     */
    private String resolveRef(final FlightId identity, final int surrogateId) {
        if (identity == null) {
            return "#" + surrogateId;
        }
        return flights.ofIdentity(identity)
                .map(flight -> flight.identity().toString())
                .orElse(identity.toString());
    }
}