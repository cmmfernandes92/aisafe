package eapli.aisafeweb.service;

import org.springframework.stereotype.Service;

import eapli.aisafe.enginemodelmanagement.application.RegisterEngineModelController;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class EngineModelCommandService {

    public void register(
            final String engineModelName,
            final String makerName,
            final MotorizationType motorizationType,
            final FuelType fuelType,
            final double power,
            final double efficiency) {
        final var controller = new RegisterEngineModelController(
                PersistenceContext.repositories().engineModels(),
                PersistenceContext.repositories().makers());
        controller.register(engineModelName, makerName, motorizationType, fuelType, power, efficiency);
    }
}
