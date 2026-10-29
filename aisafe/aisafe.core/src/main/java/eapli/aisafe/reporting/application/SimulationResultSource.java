package eapli.aisafe.reporting.application;

import java.io.IOException;
import java.nio.file.Path;

import eapli.aisafe.reporting.application.parsing.ParsedSimulationReport;

/**
 * Separated Interface (port) over the C simulator's output. The core stays
 * unaware of the report's text format: the adapter reads a run folder
 * ({@code report.txt} + the {@code flights.csv} surrogate-id sidecar, the Option A
 * integration contract) and returns a format-free {@link ParsedSimulationReport}.
 */
public interface SimulationResultSource {

    /**
     * Reads and parses the run folder produced by the egress (or a hand-authored
     * fixture pair) into a format-free report.
     *
     * @param runDir directory holding {@code report.txt} and, optionally,
     *               {@code flights.csv}
     */
    ParsedSimulationReport read(Path runDir) throws IOException;
}