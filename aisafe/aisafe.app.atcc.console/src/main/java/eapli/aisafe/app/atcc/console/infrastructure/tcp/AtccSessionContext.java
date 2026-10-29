package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import eapli.aisafe.fleetmanagement.application.AircraftDTO;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.pilotmanagement.domain.Pilot;

/**
 * Strategy interface that abstracts the eight ATCC application-layer operations
 * the handler dispatches to. Keeping this seam makes AtccConnectionHandler
 * independently testable with a stub — no controllers or persistence needed.
 */
public interface AtccSessionContext {

    // US070
    Aircraft addAircraft(String registration, Long modelId,
                         Map<SeatClass, Integer> seats, String country);

    // US071
    Aircraft decommissionAircraft(String registration);

    // US072
    List<AircraftDTO> listFleet();

    // US073
    FlightRoute createFlightRoute(String routeName, String originCode, String destCode);

    // US074
    FlightRoute deactivateFlightRoute(String routeName, LocalDate effectiveDate);

    // US075
    Pilot addPilot(String pilotEmail, List<Long> modelIds);

    // US076
    Iterable<Pilot> listPilots();

    // US077
    void deactivatePilot(String pilotEmail);
}
