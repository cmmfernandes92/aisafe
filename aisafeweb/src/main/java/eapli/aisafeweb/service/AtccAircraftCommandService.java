package eapli.aisafeweb.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.fleetmanagement.application.AddAircraftController;
import eapli.aisafe.fleetmanagement.application.DecommissionAircraftController;
import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController;
import eapli.aisafe.fleetmanagement.application.ListCompanyFleetController.FleetEntry;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class AtccAircraftCommandService {

    private AddAircraftController addController() {
        return new AddAircraftController(
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().aisafeUsers());
    }

    private DecommissionAircraftController decommissionController() {
        return new DecommissionAircraftController(
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers());
    }

    private ListCompanyFleetController listController() {
        return new ListCompanyFleetController(
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().customerCollaborators(),
                PersistenceContext.repositories().aisafeUsers());
    }

    public AircraftPageSnapshot aircraftPage() {
        return new AircraftPageSnapshot(aircraftRows(), aircraftModelOptions());
    }

    public List<AircraftRow> aircraftRows() {
        return listController().listFleet(null, null, null).stream()
                .map(this::toRow)
                .sorted(Comparator.comparing(AircraftRow::registration))
                .toList();
    }

    public List<AircraftModelOption> aircraftModelOptions() {
        return toList(addController().listModels()).stream()
                .sorted(Comparator.comparing(model -> model.modelName().toString()))
                .map(model -> new AircraftModelOption(
                        model.identity(),
                        model.modelName().toString(),
                        model.maker().identity().toString(),
                        model.maxPassengerCapacity()))
                .toList();
    }

    public void addAircraft(final String registration, final Long modelId,
            final int economySeats, final int businessSeats, final int firstClassSeats,
            final String country) {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, economySeats);
        seats.put(SeatClass.BUSINESS, businessSeats);
        seats.put(SeatClass.FIRST, firstClassSeats);
        addController().addAircraft(registration, modelId, seats, country);
    }

    public void decommissionAircraft(final String registration) {
        decommissionController().decommissionAircraft(registration);
    }

    private AircraftRow toRow(final FleetEntry entry) {
        final var aircraft = entry.aircraft();
        final var model = entry.model();
        final var cabin = aircraft.cabinConfiguration();
        return new AircraftRow(
                aircraft.registration().value(),
                model.identity(),
                model.modelName().toString(),
                model.maker().identity().toString(),
                cabin.economySeats(),
                cabin.businessSeats(),
                cabin.firstClassSeats(),
                cabin.totalSeats(),
                aircraft.registrationCountry().toString(),
                aircraft.status().name());
    }

    private static <T> List<T> toList(final Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false).toList();
    }

    public record AircraftPageSnapshot(
            List<AircraftRow> aircraftRows,
            List<AircraftModelOption> models) {
    }

    public record AircraftRow(
            String registration,
            Long modelId,
            String modelName,
            String makerName,
            int economySeats,
            int businessSeats,
            int firstClassSeats,
            int totalSeats,
            String registrationCountry,
            String status) {
    }

    public record AircraftModelOption(
            Long id,
            String modelName,
            String makerName,
            int maxPassengerCapacity) {
    }
}
