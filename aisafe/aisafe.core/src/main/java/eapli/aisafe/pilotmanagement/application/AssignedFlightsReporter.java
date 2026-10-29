package eapli.aisafe.pilotmanagement.application;

public interface AssignedFlightsReporter {
    void report(Iterable<AssignedFlightDTO> flights);
}