package eapli.aisafe.app.fco.console.presentation.areasimulation;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.CandidateFlight;
import eapli.aisafe.flightmanagement.application.areasimulation.AreaSimulationRun;
import eapli.aisafe.flightmanagement.application.areasimulation.RunAreaSimulationController;
import eapli.aisafe.flightmanagement.application.areasimulation.SimulationWindow;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RunAreaSimulationUI extends AbstractUI {

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RunAreaSimulationController controller;

    public RunAreaSimulationUI(final RunAreaSimulationController controller) {
        this.controller = controller;
    }

    @Override
    public String headline() {
        return "Run Area Simulation";
    }

    @Override
    protected boolean doShow() {
        final SimulationWindow window = readWindow();
        if (window == null) {
            return false;
        }

        final List<CandidateFlight> candidates;
        try {
            System.out.println("Assigned area: " + controller.myAreaCode());
            candidates = controller.candidateFlights(window);
        } catch (final RuntimeException e) {
            System.out.println("Unable to load candidate flights: " + e.getMessage());
            return false;
        }

        if (candidates.isEmpty()) {
            System.out.println("No TESTED candidate flights cross your area in that window.");
            return false;
        }

        printCandidates(candidates);
        final String run = Console.readLine("Run SCOMP simulation now? [Y/n]: ");
        if (!run.isBlank() && !run.equalsIgnoreCase("y") && !run.equalsIgnoreCase("yes")) {
            return false;
        }

        try {
            final AreaSimulationRun result = controller.runForMyArea(window);
            System.out.println("Simulation completed.");
            System.out.println("  Run directory: " + result.runDir().toAbsolutePath());
            System.out.println("  Report: " + result.reportPath().toAbsolutePath()
                    + existsSuffix(result.reportPath()));
            System.out.println("  Violations: " + result.violationsPath().toAbsolutePath()
                    + existsSuffix(result.violationsPath()));
            System.out.println("  Flight ID map:");
            result.idMap().forEach((nativeId, flightId) ->
                    System.out.println("    " + nativeId + " -> " + flightId));
        } catch (final IOException e) {
            System.out.println("Could not run simulation: " + e.getMessage());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Simulation interrupted.");
        } catch (final RuntimeException e) {
            System.out.println("Could not run simulation: " + e.getMessage());
        }

        return false;
    }

    private SimulationWindow readWindow() {
        final LocalDate today = LocalDate.now();
        final LocalDateTime defaultFrom = LocalDateTime.of(today, LocalTime.MIN);
        final LocalDateTime defaultTo = defaultFrom.plusDays(1);
        final LocalDateTime from = readDateTime("From [" + INPUT_FORMAT.format(defaultFrom) + "]: ",
                defaultFrom);
        final LocalDateTime to = readDateTime("To   [" + INPUT_FORMAT.format(defaultTo) + "]: ",
                defaultTo);
        try {
            return new SimulationWindow(from, to);
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid window: " + e.getMessage());
            return null;
        }
    }

    private LocalDateTime readDateTime(final String prompt, final LocalDateTime defaultValue) {
        while (true) {
            final String raw = Console.readLine(prompt);
            if (raw.isBlank()) {
                return defaultValue;
            }
            try {
                return LocalDateTime.parse(raw.trim(), INPUT_FORMAT);
            } catch (final DateTimeParseException e) {
                System.out.println("  Use format yyyy-MM-dd HH:mm.");
            }
        }
    }

    private static void printCandidates(final List<CandidateFlight> candidates) {
        System.out.println();
        System.out.printf("  %-18s %-20s %-12s %-14s %s%n",
                "Departure", "Route", "Aircraft", "Designator", "Segments");
        System.out.println("  " + "-".repeat(78));
        for (final CandidateFlight candidate : candidates) {
            System.out.printf("  %-18s %-20s %-12s %-14s %d%n",
                    candidate.departure(),
                    candidate.route(),
                    candidate.aircraft(),
                    candidate.designator(),
                    candidate.segmentsInArea());
        }
        System.out.println();
    }

    private static String existsSuffix(final java.nio.file.Path path) {
        return Files.exists(path) ? "" : " (not found)";
    }
}
