package eapli.aisafeweb.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircraftmodelmanagement.application.RegisterAircraftModelController;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class AircraftModelCommandService {

    public void register(
            final String modelName,
            final String makerName,
            final AircraftType type,
            final MotorizationType motorizationType,
            final double emptyWeight,
            final double mtow,
            final double mzfw,
            final double maxFuelCapacity,
            final double serviceCeiling,
            final double cruiseSpeed,
            final double wingArea,
            final double dragCoefficient,
            final double liftCoefficient,
            final double maximumRange,
            final List<Long> engineIds) {
        final Set<EngineModel> certifiedEngines = resolveEngines(engineIds);
        final var controller = new RegisterAircraftModelController(
                PersistenceContext.repositories().aircraftModels(),
                PersistenceContext.repositories().engineModels(),
                PersistenceContext.repositories().makers());
        controller.register(modelName, makerName, type, motorizationType,
                emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                wingArea, dragCoefficient, liftCoefficient, maximumRange, certifiedEngines);
    }

    private Set<EngineModel> resolveEngines(final List<Long> ids) {
        final Set<EngineModel> result = new HashSet<>();
        if (ids == null) return result;
        for (final Long id : ids) {
            PersistenceContext.repositories().engineModels().ofIdentity(id)
                    .ifPresent(result::add);
        }
        return result;
    }
}
