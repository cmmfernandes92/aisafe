package eapli.aisafe.infrastructure.bootstrapers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.enginemodelmanagement.application.RegisterEngineModelController;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class EngineModelBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineModelBootstrapper.class);

    private final RegisterEngineModelController controller = new RegisterEngineModelController(
            PersistenceContext.repositories().engineModels(),
            PersistenceContext.repositories().makers());

    @Override
    public boolean execute() {
        // power = static (sea-level) thrust; cruiseThrust = thrust at cruise Mach
        // (~20% of static for turbofans) — the two §3.3 thrust points (US085).
        register("CFM56-5B", "CFM International", MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 22000, 0.545);
        register("CFM LEAP-1A", "CFM International", MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
        register("Trent 700", "Rolls-Royce", MotorizationType.TURBOFAN, FuelType.JET_A, 316000, 62000, 0.562);
        register("PW1100G", "Pratt & Whitney", MotorizationType.TURBOFAN, FuelType.JET_A, 120000, 23000, 0.50);
        register("GEnx-1B", "GE Aviation", MotorizationType.TURBOFAN, FuelType.JET_A, 339000, 64000, 0.512);
        return true;
    }

    private void register(final String name, final String makerName,
            final MotorizationType type, final FuelType fuel,
            final double power, final double cruiseThrust, final double efficiency) {
        try {
            controller.register(name, makerName, type, fuel, power, cruiseThrust, efficiency);
            LOGGER.debug("Registered engine model: {} ({})", name, makerName);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming engine model '{}' already exists", name);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Failed to register engine model '{}': {}", name, e.getMessage());
        }
    }
}
