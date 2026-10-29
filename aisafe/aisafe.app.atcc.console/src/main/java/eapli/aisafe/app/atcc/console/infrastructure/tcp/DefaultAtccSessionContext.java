package eapli.aisafe.app.atcc.console.infrastructure.tcp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eapli.aisafe.fleetmanagement.application.AddAircraftController;
import eapli.aisafe.fleetmanagement.application.AircraftDTO;
import eapli.aisafe.fleetmanagement.application.DecommissionAircraftController;
import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.flightroutemanagement.application.CreateFlightRouteController;
import eapli.aisafe.flightroutemanagement.application.DeactivateFlightRouteController;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilotmanagement.application.AddPilotController;
import eapli.aisafe.pilotmanagement.application.AssignedFlightsReporter;
import eapli.aisafe.pilotmanagement.application.DeactivatePilotController;
import eapli.aisafe.pilotmanagement.application.ListPilotController;
import eapli.aisafe.pilotmanagement.domain.Pilot;

public class DefaultAtccSessionContext implements AtccSessionContext {

    @Override
    public Aircraft addAircraft(final String registration, final Long modelId,
                                final Map<SeatClass, Integer> seats, final String country) {
        return new AddAircraftController(
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().aisafeUsers())
                .addAircraft(registration, modelId, seats, country);
    }

    @Override
    public Aircraft decommissionAircraft(final String registration) {
        return new DecommissionAircraftController(
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers())
                .decommissionAircraft(registration);
    }

    @Override
    public List<AircraftDTO> listFleet() {
        final List<AircraftDTO> result = new ArrayList<>();
        for (final ListCompanyFleetController.FleetEntry entry :
                new ListCompanyFleetController(
                        PersistenceContext.repositories().aircraft(),
                        PersistenceContext.repositories().aircraftModels(),
                        PersistenceContext.repositories().customerCollaborators(),
                        PersistenceContext.repositories().aisafeUsers())
                        .listFleet(null, null, null)) {
            result.add(new AircraftDTO(
                    entry.aircraft().registration().value(),
                    entry.model().modelName().toString(),
                    entry.aircraft().status().name(),
                    entry.model().maxPassengerCapacity()));
        }
        return result;
    }

    @Override
    public FlightRoute createFlightRoute(final String routeName,
                                         final String originCode, final String destCode) {
        return new CreateFlightRouteController(
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers())
                .createFlightRoute(routeName, originCode, destCode);
    }

    @Override
    public FlightRoute deactivateFlightRoute(final String routeName,
                                             final LocalDate effectiveDate) {
        return new DeactivateFlightRouteController(
                PersistenceContext.repositories().flightRoutes(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers())
                .deactivateFlightRoute(routeName, effectiveDate);
    }

    @Override
    public Pilot addPilot(final String pilotEmail, final List<Long> modelIds) {
        final Set<Long> modelIdSet = new LinkedHashSet<>(modelIds);
        return new AddPilotController(
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().aisafeUsers(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().customerCollaborators())
                .addPilot(pilotEmail, modelIdSet);
    }

    @Override
    public Iterable<Pilot> listPilots() {
        return new ListPilotController().listPilots();
    }

    @Override
    public void deactivatePilot(final String pilotEmail) {
        // Capturing reporter: collects assigned flights so the handler can
        // include them in the error message if deactivation is blocked.
        final AssignedFlightsReporter reporter = flights -> { /* no-op: handler catches the exception */ };

        new DeactivatePilotController(
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().aisafeUsers(),
                PersistenceContext.repositories().customerCollaborators())
                .deactivatePilot(pilotEmail, reporter);
    }
}