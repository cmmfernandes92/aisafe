package eapli.aisafe.flightmanagement.application.areasimulation;

import java.time.LocalDateTime;

public record SimulationWindow(LocalDateTime from, LocalDateTime to) {

    public SimulationWindow {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Simulation window bounds must not be null");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Simulation window start must be before end");
        }
    }
}
