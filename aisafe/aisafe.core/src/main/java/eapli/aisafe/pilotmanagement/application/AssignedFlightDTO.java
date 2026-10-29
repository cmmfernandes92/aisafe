package eapli.aisafe.pilotmanagement.application;

import java.time.LocalDate;

public record AssignedFlightDTO(String routeName, LocalDate departureDate, String aircraftRegistration) {}