package eapli.aisafeweb.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.pilotmanagement.application.AddPilotController;
import eapli.aisafe.pilotmanagement.application.AssignedFlightDTO;
import eapli.aisafe.pilotmanagement.application.DeactivatePilotController;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.AisafeRoles;

@Service
public class AtccPilotCommandService {

    private AddPilotController addController() {
        return new AddPilotController(
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().aisafeUsers(),
                PersistenceContext.repositories().airTransportCompanies(),
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().customerCollaborators());
    }

    private DeactivatePilotController deactivateController() {
        return new DeactivatePilotController(
                PersistenceContext.repositories().pilots(),
                PersistenceContext.repositories().flights(),
                PersistenceContext.repositories().aisafeUsers(),
                PersistenceContext.repositories().customerCollaborators());
    }

    public PilotPageSnapshot pilotPage() {
        return new PilotPageSnapshot(pilotRows(), pilotUserOptions(), aircraftModelOptions());
    }

    public List<PilotRow> pilotRows() {
        return toList(deactivateController().listActivePilotsOfMyCompany()).stream()
                .map(this::toRow)
                .sorted(Comparator.comparing(PilotRow::email))
                .toList();
    }

    public void addPilot(final String pilotEmail, final List<Long> modelIds) {
        final Set<Long> certifications = modelIds == null
                ? Set.of()
                : new LinkedHashSet<>(modelIds);
        addController().addPilot(pilotEmail, certifications);
    }

    public void deactivatePilot(final String pilotEmail) {
        final List<AssignedFlightDTO> assignedFlights = new ArrayList<>();
        try {
            deactivateController().deactivatePilot(pilotEmail, flights -> flights.forEach(assignedFlights::add));
        } catch (final RuntimeException ex) {
            if (!assignedFlights.isEmpty()) {
                throw new IllegalStateException(ex.getMessage() + " " + assignedFlightsSummary(assignedFlights), ex);
            }
            throw ex;
        }
    }

    private List<PilotUserOption> pilotUserOptions() {
        return toList(PersistenceContext.repositories().aisafeUsers().findAll()).stream()
                .filter(user -> user.hasRole(AisafeRoles.PILOT))
                .sorted(Comparator.comparing(user -> user.identity().toString()))
                .map(user -> new PilotUserOption(user.identity().toString(), user.phoneNumber().toString()))
                .toList();
    }

    private List<AircraftModelOption> aircraftModelOptions() {
        return toList(PersistenceContext.repositories().aircraftModels().findAll()).stream()
                .sorted(Comparator.comparing(model -> model.modelName().toString()))
                .map(model -> new AircraftModelOption(
                        model.identity(),
                        model.modelName().toString(),
                        model.maker().identity().toString()))
                .toList();
    }

    private PilotRow toRow(final Pilot pilot) {
        return new PilotRow(
                pilot.identity().toString(),
                pilot.companyCode().toString(),
                pilot.certifications().stream()
                        .map(AircraftModel::modelName)
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.joining(", ")),
                pilot.status().name());
    }

    private static String assignedFlightsSummary(final List<AssignedFlightDTO> flights) {
        final String details = flights.stream()
                .map(f -> f.routeName() + " " + f.departureDate() + " " + f.aircraftRegistration())
                .collect(Collectors.joining("; "));
        return "Assigned flights: " + details;
    }

    private static <T> List<T> toList(final Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false).toList();
    }

    public record PilotPageSnapshot(
            List<PilotRow> pilots,
            List<PilotUserOption> pilotUsers,
            List<AircraftModelOption> models) {
    }

    public record PilotRow(
            String email,
            String companyCode,
            String certifications,
            String status) {
    }

    public record PilotUserOption(
            String email,
            String phoneNumber) {
    }

    public record AircraftModelOption(
            Long id,
            String modelName,
            String makerName) {
    }
}
