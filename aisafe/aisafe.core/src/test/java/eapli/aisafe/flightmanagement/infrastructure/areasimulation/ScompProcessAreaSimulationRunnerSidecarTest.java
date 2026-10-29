package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationScenario;
import eapli.aisafe.flightmanagement.application.areasimulation.FlightProfile;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioFlight;
import eapli.aisafe.flightmanagement.application.areasimulation.ScenarioSegment;
import eapli.aisafe.flightmanagement.application.areasimulation.SimParams;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;

public class ScompProcessAreaSimulationRunnerSidecarTest {

    @Test
    public void ensureRunWritesIdMapSidecarBesideScenario() throws IOException, InterruptedException {
        final Path runDir = Files.createTempDirectory("aisafe-run");
        try {
            // the binary is intentionally absent: run() writes scenario.json + flights.csv
            // first, then fails on the missing binary. We assert the sidecar landed.
            final var runner = new ScompProcessAreaSimulationRunner(
                    runDir.resolve("no-such-flight_sim"),
                    new ScompScenarioJsonWriter(),
                    Duration.ofSeconds(5));

            final AreaSimulationScenario scenario = new AreaSimulationScenario(
                    new SimParams("LPPC-202606121200", 10, 60.0, 2, 3, 8.0, 600.0,
                            -1.0, 1.0, -2.0, 2.0, 0.0, 20000.0, 38.5, -9.0),
                    List.of(new ScenarioFlight(1, FlightProfile.defaultJet(),
                            List.of(new ScenarioSegment("cruise",
                                    38.5, -9.0, 9000.0,
                                    38.6, -8.9, 9000.0)), 0)),
                    Map.of(1, new FlightId(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12))));

            try {
                runner.run(scenario, runDir);
                fail("expected failure on missing binary");
            } catch (final IllegalStateException expected) {
                // binary missing — the run folder was still populated
            }

            final Path sidecar = runDir.resolve(SurrogateIdMapWriter.FILE_NAME);
            assertTrue("flights.csv should exist in the run folder", Files.exists(sidecar));
            assertTrue(Files.readString(sidecar).contains("1,TP123,2026-06-12"));
        } finally {
            deleteRecursively(runDir);
        }
    }

    private static void deleteRecursively(final Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (final IOException ignored) {
                    // best-effort temp cleanup
                }
            });
        }
    }
}