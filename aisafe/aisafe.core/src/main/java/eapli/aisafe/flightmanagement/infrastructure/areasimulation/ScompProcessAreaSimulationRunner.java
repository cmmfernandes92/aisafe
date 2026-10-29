package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationRun;
import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationRunner;
import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationScenario;
import eapli.framework.validations.Preconditions;

public class ScompProcessAreaSimulationRunner implements AreaSimulationRunner {

    private static final Object RUN_LOCK = new Object();

    private final Path binaryPath;
    private final ScompScenarioJsonWriter writer;
    private final SurrogateIdMapWriter idMapWriter;
    private final Duration timeout;

    public ScompProcessAreaSimulationRunner(final Path binaryPath,
            final ScompScenarioJsonWriter writer,
            final Duration timeout) {
        Preconditions.noneNull(binaryPath, writer, timeout);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Simulation timeout must be positive");
        }
        this.binaryPath = binaryPath.toAbsolutePath().normalize();
        this.writer = writer;
        this.idMapWriter = new SurrogateIdMapWriter();
        this.timeout = timeout;
    }

    @Override
    public AreaSimulationRun run(final AreaSimulationScenario scenario, final Path runDir)
            throws IOException, InterruptedException {
        Preconditions.noneNull(scenario, runDir);
        synchronized (RUN_LOCK) {
            final Path scenarioPath = writer.write(scenario, runDir);
            // US111 integration contract (Option A): record the surrogate-id reverse
            // map on disk so the ingest can join report.txt flight ids to domain flights.
            idMapWriter.write(scenario.idMap(), runDir);
            if (!Files.isExecutable(binaryPath)) {
                throw new IllegalStateException("SCOMP flight_sim binary not found or not executable at "
                        + binaryPath + ". Run `make` in " + binaryPath.getParent()
                        + ". Scenario written to " + scenarioPath);
            }

            final ProcessBuilder pb = new ProcessBuilder(binaryPath.toString(),
                    scenarioPath.getFileName().toString());
            pb.directory(runDir.toFile());
            pb.redirectErrorStream(true);

            final Process process = pb.start();
            final StringBuilder output = new StringBuilder();
            final Thread reader = new Thread(() -> readOutput(process, output), "scomp-flight-sim-output");
            reader.start();

            final boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                reader.join();
                throw new IllegalStateException("SCOMP simulation timed out after "
                        + timeout.toSeconds() + " seconds. Output:\n" + output);
            }

            reader.join();
            final int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("SCOMP simulation failed with exit code "
                        + exitCode + ". Output:\n" + output);
            }

            return new AreaSimulationRun(
                    runDir,
                    runDir.resolve("report.txt"),
                    runDir.resolve("violations.log"),
                    exitCode,
                    scenario.idMap());
        }
    }

    private static void readOutput(final Process process, final StringBuilder output) {
        try (var in = process.getInputStream()) {
            output.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            output.append("\nCould not read SCOMP output: ").append(e.getMessage());
        }
    }
}
