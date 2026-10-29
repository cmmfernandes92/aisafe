package eapli.aisafe.app.pilot.console.presentation.flightplan;

import java.time.LocalDate;

import eapli.aisafe.flightmanagement.application.SimulatorV1FlightPlanTestService;
import eapli.aisafe.flightmanagement.application.TestFlightPlanController;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * US085 — the pilot tests/validates the current plan of one of their flights.
 * It wires {@link TestFlightPlanController} with the production
 * {@link SimulatorV1FlightPlanTestService}: the {@link ConsoleFlightPlanCompiler}
 * recompiles the stored DSL, the pilot's attached weather is overlaid, and the
 * §3.3 physics decides the verdict. On success the plan goes {@code VALIDATED →
 * TESTED}; on failure the reasons are printed and the plan state is unchanged.
 */
public class TestFlightPlanUI extends AbstractUI {

    private final TestFlightPlanController controller = new TestFlightPlanController(
            PersistenceContext.repositories().flights(),
            new SimulatorV1FlightPlanTestService(
                    new ConsoleFlightPlanCompiler(),
                    PersistenceContext.repositories().weatherData(),
                    PersistenceContext.repositories().aircraft(),
                    PersistenceContext.repositories().aircraftModels()));

    @Override
    protected boolean doShow() {
        System.out.println("\n=== Test / Validate Flight Plan (US085) ===");
        final String route = Console.readLine("  Route name").trim();
        final String dateInput = Console.readLine("  Departure date (YYYY-MM-DD)").trim();

        final FlightId flightId;
        try {
            flightId = new FlightId(RouteName.valueOf(route), LocalDate.parse(dateInput));
        } catch (final RuntimeException e) {
            System.out.println("  [FAIL] Invalid route or date: " + e.getMessage());
            return false;
        }

        try {
            final FlightPlanTestResult result = controller.testFlightPlan(flightId);
            if (result.isSuccess()) {
                System.out.println("\n  [ OK ] Plan promoted to TESTED.");
                System.out.println("         " + result.message());
            } else {
                System.out.println("\n  [FAIL] Plan did not pass — state left unchanged:");
                System.out.println("         " + result.message());
            }
        } catch (final RuntimeException e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Test / Validate Flight Plan";
    }
}
