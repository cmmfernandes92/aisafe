package eapli.aisafeweb.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import eapli.aisafe.flightmanagement.application.SimulatorV1FlightPlanTestService;
import eapli.aisafe.flightmanagement.application.TestFlightPlanController;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.dsl.DslFlightPlanCompiler;

/**
 * Web adapter for US085 (Test/Validate Flight Plan). It wires the core
 * {@link TestFlightPlanController} with the production
 * {@link SimulatorV1FlightPlanTestService} — the same recompile + per-segment
 * weather overlay + §3.3 physics the pilot console runs. On success the plan is
 * promoted {@code VALIDATED → TESTED}; on failure the plan state is unchanged and
 * the reasons are reported. The caller must have authenticated the pilot for the
 * core controller call (the controller enforces the PILOT role and flight ownership).
 */
@Service
public class FlightPlanTestCommandService {

    private final DslFlightPlanCompiler compiler;

    public FlightPlanTestCommandService(final DslFlightPlanCompiler compiler) {
        this.compiler = compiler;
    }

    /** The verdict of a test run, mapped from the domain {@link FlightPlanTestResult}. */
    public record TestOutcome(boolean success, String message) {
    }

    public TestOutcome test(final String route, final LocalDate date) {
        final TestFlightPlanController controller = new TestFlightPlanController(
                PersistenceContext.repositories().flights(),
                new SimulatorV1FlightPlanTestService(
                        compiler,
                        PersistenceContext.repositories().weatherData(),
                        PersistenceContext.repositories().aircraft(),
                        PersistenceContext.repositories().aircraftModels()));

        final FlightPlanTestResult result =
                controller.testFlightPlan(new FlightId(RouteName.valueOf(route), date));
        return new TestOutcome(result.isSuccess(), result.message());
    }
}