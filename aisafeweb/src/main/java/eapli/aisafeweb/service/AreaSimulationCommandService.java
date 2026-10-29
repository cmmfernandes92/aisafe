package eapli.aisafeweb.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.CandidateFlight;
import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationRun;
import eapli.aisafe.flightmanagement.application.areasimulation.RunAreaSimulationController;
import eapli.aisafe.flightmanagement.application.areasimulation.SimulationWindow;
import eapli.aisafe.flightmanagement.infrastructure.areasimulation.ScompProcessAreaSimulationRunner;
import eapli.aisafe.flightmanagement.infrastructure.areasimulation.ScompScenarioJsonWriter;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.dsl.DslFlightPlanCompiler;
import eapli.aisafeweb.service.FcoAreaQueryService.AreaView;

@Service
public class AreaSimulationCommandService {

    private final FcoAreaQueryService fcoQueries;
    private final DslFlightPlanCompiler compiler;
    private final Path binaryPath;
    private final Path runsRoot;
    private final Duration timeout;

    public AreaSimulationCommandService(final FcoAreaQueryService fcoQueries,
            final DslFlightPlanCompiler compiler,
            @Value("${aisafeweb.scomp.binary:scomp/sprint-3/flight_sim/flight_sim}") final String binaryPath,
            @Value("${aisafeweb.scomp.runsRoot:scomp/sprint-3/flight_sim/runs}") final String runsRoot,
            @Value("${aisafeweb.scomp.timeoutSeconds:120}") final long timeoutSeconds) {
        this.fcoQueries = fcoQueries;
        this.compiler = compiler;
        this.binaryPath = resolveConfiguredPath(binaryPath);
        this.runsRoot = resolveConfiguredPath(runsRoot);
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public Optional<AreaView> area(final String email) {
        return fcoQueries.resolveArea(email);
    }

    public List<CandidateFlight> candidates(final String email, final SimulationWindow window) {
        if (window == null) {
            return List.of();
        }
        return area(email)
                .map(area -> fcoQueries.candidatesForArea(
                        AreaCode.valueOf(area.code()), window.from(), window.to()))
                .orElseGet(List::of);
    }

    public AreaSimulationRun run(final SimulationWindow window) throws IOException, InterruptedException {
        return controller().runForMyArea(window);
    }

    private RunAreaSimulationController controller() {
        final ScompProcessAreaSimulationRunner runner = new ScompProcessAreaSimulationRunner(
                binaryPath, new ScompScenarioJsonWriter(), timeout);
        return new RunAreaSimulationController(
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().customerCollaborators(),
                compiler,
                runner,
                runsRoot);
    }

    private static Path resolveConfiguredPath(final String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("SCOMP path configuration must not be blank");
        }
        final Path path = Path.of(configured.strip());
        if (path.isAbsolute()) {
            return path.normalize();
        }

        final Path cwd = Path.of("").toAbsolutePath().normalize();
        final Path direct = cwd.resolve(path).normalize();
        if (Files.exists(direct)) {
            return direct;
        }
        final Path parent = cwd.getParent() == null ? direct : cwd.getParent().resolve(path).normalize();
        if (Files.exists(parent)) {
            return parent;
        }
        return direct;
    }
}
