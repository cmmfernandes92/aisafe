package eapli.aisafe.infrastructure.bootstrapers;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.aisafe.aircraftmodelmanagement.application.RegisterAircraftModelController;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class AircraftModelBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftModelBootstrapper.class);

    private final RegisterAircraftModelController controller = new RegisterAircraftModelController(
            PersistenceContext.repositories().aircraftModels(),
            PersistenceContext.repositories().engineModels(),
            PersistenceContext.repositories().makers());

    private final EngineModelRepository engineRepo = PersistenceContext.repositories().engineModels();
    private final MakerRepository makerRepo = PersistenceContext.repositories().makers();

    @Override
    public boolean execute() {
        final Maker cfm = makerRepo.findByName(MakerName.valueOf("CFM International")).orElse(null);
        final Maker ge = makerRepo.findByName(MakerName.valueOf("GE Aviation")).orElse(null);

        if (cfm == null || ge == null) {
            LOGGER.warn("Required makers not found; skipping aircraft model bootstrap");
            return false;
        }

        final EngineModel cfm56 = engineRepo.findByNameAndMaker(EngineModelName.valueOf("CFM56-5B"), cfm).orElse(null);
        final EngineModel leap = engineRepo.findByNameAndMaker(EngineModelName.valueOf("CFM LEAP-1A"), cfm).orElse(null);
        final EngineModel genx = engineRepo.findByNameAndMaker(EngineModelName.valueOf("GEnx-1B"), ge).orElse(null);

        // wingArea, wingSpan: real published geometry → aspect ratio AR = span²/area
        // for the §3.3 induced-drag term (US085). A320neo span 35.8 m (AR ≈ 10.45);
        // 787-9 span 60.12 m (AR ≈ 10.0).
        if (cfm56 != null && leap != null) {
            register("A320neo", "Airbus", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                    42600, 79000, 64300, 26730, 12131, 833, 122.6, 35.8, 0.024, 1.5, 6300, Set.of(cfm56, leap));
        }
        if (genx != null) {
            register("787-9", "Boeing", AircraftType.PASSENGER, MotorizationType.TURBOFAN,
                    128850, 254011, 192777, 126920, 13137, 903, 360.0, 60.12, 0.022, 1.4, 14140, Set.of(genx));
        }
        return true;
    }

    private void register(final String modelName, final String makerName,
            final AircraftType type, final MotorizationType motorizationType,
            final double emptyWeight, final double mtow, final double mzfw,
            final double maxFuelCapacity, final double serviceCeiling, final double cruiseSpeed,
            final double wingArea, final double wingSpan, final double dragCoefficient,
            final double liftCoefficient, final double maximumRange, final Set<EngineModel> engines) {
        try {
            controller.register(modelName, makerName, type, motorizationType,
                    emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                    wingArea, wingSpan, dragCoefficient, liftCoefficient, maximumRange, engines);
            LOGGER.debug("Registered aircraft model: {} ({})", modelName, makerName);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.debug("Assuming aircraft model '{}' already exists", modelName);
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Failed to register aircraft model '{}': {}", modelName, e.getMessage());
        }
    }
}
