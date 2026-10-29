package eapli.aisafe.app.fco.console.presentation.areasimulation;

import java.nio.file.Path;
import java.time.Duration;

import eapli.aisafe.flightmanagement.application.areasimulation.RunAreaSimulationController;
import eapli.aisafe.flightmanagement.infrastructure.areasimulation.ScompProcessAreaSimulationRunner;
import eapli.aisafe.flightmanagement.infrastructure.areasimulation.ScompScenarioJsonWriter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;

public class RunAreaSimulationAction implements Action {

    @Override
    public boolean execute() {
        final Path flightSimDir = projectRoot().resolve("scomp/sprint-3/flight_sim");
        final ScompScenarioJsonWriter writer = new ScompScenarioJsonWriter();
        final ScompProcessAreaSimulationRunner runner = new ScompProcessAreaSimulationRunner(
                flightSimDir.resolve("flight_sim"),
                writer,
                Duration.ofMinutes(2));

        final RunAreaSimulationController controller = new RunAreaSimulationController(
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().customerCollaborators(),
                new FcoFlightPlanCompiler(),
                runner,
                flightSimDir.resolve("runs"));

        return new RunAreaSimulationUI(controller).show();
    }

    private static Path projectRoot() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !root.resolve("scomp/sprint-3/flight_sim").toFile().isDirectory()) {
            root = root.getParent();
        }
        return root != null ? root : Path.of(".").toAbsolutePath();
    }
}
