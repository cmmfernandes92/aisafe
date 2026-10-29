package eapli.aisafe.aircraftmodelmanagement.application;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.framework.application.UseCaseController;

@UseCaseController
public class AddEngineToAircraftModelController {

    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;

    public AddEngineToAircraftModelController(final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
    }

    public AircraftModel addEngine(final Long aircraftModelId, final Long engineModelId) {
        final AircraftModel aircraft = aircraftModelRepository.ofIdentity(aircraftModelId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelId));
        final EngineModel engine = engineModelRepository.ofIdentity(engineModelId)
                .orElseThrow(() -> new IllegalArgumentException("Engine model not found: " + engineModelId));

        aircraft.addCertifiedEngine(engine);
        return aircraftModelRepository.save(aircraft);
    }

    public Iterable<AircraftModel> listAircraftModels() {
        return aircraftModelRepository.findAll();
    }

    public Iterable<EngineModel> listCompatibleEngines(final Long aircraftModelId) {
        final AircraftModel aircraft = aircraftModelRepository.ofIdentity(aircraftModelId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + aircraftModelId));
        return engineModelRepository.findByMotorizationType(aircraft.motorizationType());
    }
}
