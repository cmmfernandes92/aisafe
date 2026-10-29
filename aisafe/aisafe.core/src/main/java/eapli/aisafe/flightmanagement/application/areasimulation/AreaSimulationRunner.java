package eapli.aisafe.flightmanagement.application.areasimulation;

import java.io.IOException;
import java.nio.file.Path;

public interface AreaSimulationRunner {

    AreaSimulationRun run(AreaSimulationScenario scenario, Path runDir)
            throws IOException, InterruptedException;
}
