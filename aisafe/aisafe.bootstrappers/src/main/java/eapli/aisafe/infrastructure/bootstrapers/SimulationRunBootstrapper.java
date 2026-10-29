package eapli.aisafe.infrastructure.bootstrapers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.reporting.domain.FlightExecutionStatus;
import eapli.aisafe.reporting.domain.FlightOutcome;
import eapli.aisafe.reporting.domain.InvolvedFlight;
import eapli.aisafe.reporting.domain.SafetyViolation;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.ValidationOutcome;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

/**
 * Seeds historical {@link SimulationRun} records (US111) for the Lisboa FIR
 * (mainland Portugal, {@code LPPC} — the area that owns the LIS/OPO airports) so
 * US112's monthly statistics report has data to aggregate.
 *
 * <p>Runs span <strong>January through May 2026</strong> with deliberate variance
 * (different flight counts, mixes of {@code COMPLETED}/{@code TERMINATED_BY_VIOLATION}
 * outcomes, zero-to-several violations, and both PASS and FAIL verdicts). June 2026
 * is intentionally left empty — it is the live demo window for generating fresh runs.
 *
 * <p>Idempotent: if {@code LPPC} already carries any persisted run the bootstrapper
 * does nothing, since each {@link SimulationRun} is stamped with a fresh random id
 * and would otherwise duplicate on every launch.
 */
@SuppressWarnings("squid:S106")
public class SimulationRunBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationRunBootstrapper.class);

    /** Mainland Portugal control area (Lisboa FIR), per the ACA bootstrap. */
    private static final AreaCode LPPC = AreaCode.valueOf("LPPC");

    private static final double SECONDS_PER_STEP = 1.0;

    private final SimulationRunRepository repository = PersistenceContext.repositories().simulationRuns();

    private int seeded;

    @Override
    public boolean execute() {
        if (repository.findByAreaCode(LPPC).iterator().hasNext()) {
            LOGGER.debug("LPPC already carries simulation runs — skipping simulation-run seeding");
            return true;
        }

        // --- January 2026 ---
        run(at(2026, 1, 12, 9, 15), "clean", ValidationOutcome.PASS,
                statuses(d(2026, 1, 12), "TP101", "FR401", "LH501", "TP202"),
                List.of());
        run(at(2026, 1, 23, 14, 40), "violation", ValidationOutcome.PASS,
                statuses(d(2026, 1, 23), "TP303", "FR402", "LH502"),
                List.of(viol(at(2026, 1, 23, 14, 40), 318, 1, 2, "TP303", "FR402", 1)));

        // --- February 2026 ---
        run(at(2026, 2, 5, 8, 0), "clean", ValidationOutcome.PASS,
                statuses(d(2026, 2, 5), "TP101", "TP202", "FR401", "LH501", "LH502"),
                List.of());
        run(at(2026, 2, 17, 11, 30), "violation", ValidationOutcome.PASS,
                statuses(d(2026, 2, 17), "TP303", "FR402", "TP404", "LH501"),
                List.of(
                        viol(at(2026, 2, 17, 11, 30), 142, 1, 2, "TP303", "FR402", 2),
                        viol(at(2026, 2, 17, 11, 30), 605, 3, 4, "TP404", "LH501", 3)));
        run(at(2026, 2, 26, 16, 10), "threshold", ValidationOutcome.FAIL,
                List.of(ok(1, ref("TP101", d(2026, 2, 26))),
                        terminated(2, ref("FR401", d(2026, 2, 26)))),
                List.of(
                        viol(at(2026, 2, 26, 16, 10), 88, 1, 2, "TP101", "FR401", 1),
                        viol(at(2026, 2, 26, 16, 10), 210, 1, 2, "TP101", "FR401", 2),
                        viol(at(2026, 2, 26, 16, 10), 333, 1, 2, "TP101", "FR401", 4)));

        // --- March 2026 ---
        run(at(2026, 3, 3, 7, 45), "clean", ValidationOutcome.PASS,
                statuses(d(2026, 3, 3), "TP101", "TP202", "TP303", "FR401", "LH501", "LH502"),
                List.of());
        run(at(2026, 3, 14, 13, 20), "violation", ValidationOutcome.PASS,
                statuses(d(2026, 3, 14), "FR402", "TP404", "LH502"),
                List.of(viol(at(2026, 3, 14, 13, 20), 451, 2, 3, "TP404", "LH502", 2)));
        run(at(2026, 3, 28, 18, 5), "threshold", ValidationOutcome.FAIL,
                List.of(ok(1, ref("TP202", d(2026, 3, 28))),
                        terminated(2, ref("LH501", d(2026, 3, 28))),
                        ok(3, ref("FR401", d(2026, 3, 28)))),
                List.of(
                        viol(at(2026, 3, 28, 18, 5), 120, 2, 3, "LH501", "FR401", 1),
                        viol(at(2026, 3, 28, 18, 5), 240, 2, 3, "LH501", "FR401", 2),
                        viol(at(2026, 3, 28, 18, 5), 360, 2, 3, "LH501", "FR401", 3),
                        viol(at(2026, 3, 28, 18, 5), 480, 1, 2, "TP202", "LH501", 4)));

        // --- April 2026 ---
        run(at(2026, 4, 8, 10, 10), "clean", ValidationOutcome.PASS,
                statuses(d(2026, 4, 8), "TP101", "FR401", "LH501", "TP404"),
                List.of());
        run(at(2026, 4, 21, 15, 55), "violation", ValidationOutcome.PASS,
                statuses(d(2026, 4, 21), "TP303", "TP202", "FR402", "LH502", "TP101"),
                List.of(
                        viol(at(2026, 4, 21, 15, 55), 175, 1, 3, "TP303", "FR402", 1),
                        viol(at(2026, 4, 21, 15, 55), 540, 4, 5, "LH502", "TP101", 3)));

        // --- May 2026 ---
        run(at(2026, 5, 6, 9, 0), "clean", ValidationOutcome.PASS,
                statuses(d(2026, 5, 6), "TP101", "TP202", "FR401", "LH501", "TP404"),
                List.of());
        run(at(2026, 5, 19, 12, 35), "violation", ValidationOutcome.PASS,
                statuses(d(2026, 5, 19), "FR402", "LH502", "TP303", "TP404"),
                List.of(viol(at(2026, 5, 19, 12, 35), 388, 1, 2, "FR402", "LH502", 2)));
        run(at(2026, 5, 30, 17, 25), "threshold", ValidationOutcome.FAIL,
                List.of(terminated(1, ref("TP101", d(2026, 5, 30))),
                        ok(2, ref("FR401", d(2026, 5, 30)))),
                List.of(
                        viol(at(2026, 5, 30, 17, 25), 96, 1, 2, "TP101", "FR401", 1),
                        viol(at(2026, 5, 30, 17, 25), 198, 1, 2, "TP101", "FR401", 3),
                        viol(at(2026, 5, 30, 17, 25), 305, 1, 2, "TP101", "FR401", 4)));

        LOGGER.debug("Seeded {} simulation runs for LPPC (Jan–May 2026)", seeded);
        return true;
    }

    private void run(final LocalDateTime timestamp, final String scenario, final ValidationOutcome verdict,
            final List<FlightExecutionStatus> flightStatuses, final List<SafetyViolation> violations) {
        final String endReason = verdict == ValidationOutcome.FAIL ? "threshold_exceeded" : "completed";
        try {
            final SimulationRun simulationRun = new SimulationRun(LPPC, timestamp, scenario,
                    flightStatuses.size(), SECONDS_PER_STEP, endReason, verdict, flightStatuses, violations);
            repository.save(simulationRun);
            seeded++;
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming simulation run @{} already exists", timestamp);
        }
    }

    /** One COMPLETED status per route, all departing on the run's date. */
    private List<FlightExecutionStatus> statuses(final LocalDate departure, final String... routes) {
        final FlightExecutionStatus[] result = new FlightExecutionStatus[routes.length];
        for (int i = 0; i < routes.length; i++) {
            result[i] = ok(i + 1, ref(routes[i], departure));
        }
        return List.of(result);
    }

    private FlightExecutionStatus ok(final int surrogateId, final String flightRef) {
        return new FlightExecutionStatus(surrogateId, flightRef, FlightOutcome.COMPLETED, 0);
    }

    private FlightExecutionStatus terminated(final int surrogateId, final String flightRef) {
        return new FlightExecutionStatus(surrogateId, flightRef, FlightOutcome.TERMINATED_BY_VIOLATION, 2);
    }

    /**
     * A pairwise loss-of-separation event. Coordinates are representative values in
     * the simulator's projected local frame (metres); {@code seed} nudges them so
     * successive violations in a run do not all sit on the same point. The timestamp
     * is derived as {@code run_timestamp + step × seconds_per_step} (US111.3).
     */
    // CHECKSTYLE:OFF a pairwise violation carries a fixed, irreducible field set
    private SafetyViolation viol(final LocalDateTime runTimestamp, final int step,
            final int surrogateId1, final int surrogateId2,
            final String flightRef1, final String flightRef2, final int seed) {
        // CHECKSTYLE:ON
        final LocalDateTime timestamp = runTimestamp.plusSeconds(Math.round(step * SECONDS_PER_STEP));
        final double x1 = 11000 + seed * 250.0;
        final double y1 = 8000 + seed * 40.0;
        final double alt1 = 10000 + seed * 30.0;
        final InvolvedFlight a = new InvolvedFlight(surrogateId1, flightRef1, x1, y1, alt1, 235.0, 5.0, 0.0);
        final InvolvedFlight b = new InvolvedFlight(surrogateId2, flightRef2,
                x1 + 280.0, y1 + 120.0, alt1 + 45.0, -230.0, 8.0, 1.0);
        return SafetyViolation.separation(step, timestamp, a, b, 300.0 + seed * 12.0, 40.0 + seed * 5.0);
    }

    private String ref(final String route, final LocalDate departure) {
        return route + "@" + departure;
    }

    private LocalDateTime at(final int year, final int month, final int day, final int hour, final int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private LocalDate d(final int year, final int month, final int day) {
        return LocalDate.of(year, month, day);
    }
}