package eapli.aisafe.aircraftmodelmanagement.application;

import java.util.Set;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;

@UseCaseController
public class RegisterAircraftModelController {

    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;
    private final MakerRepository makerRepository;

    public RegisterAircraftModelController(final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository,
            final MakerRepository makerRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
        this.makerRepository = makerRepository;
    }

    /**
     * Full registration (US085): includes {@code wingSpan} so the model can
     * compute its aspect ratio for the §3.3 flight-plan physics test.
     */
    public AircraftModel register(final String modelName, final String makerName,
            final AircraftType type, final MotorizationType motorizationType,
            final double emptyWeight, final double mtow, final double mzfw,
            final double maxFuelCapacity, final double serviceCeiling, final double cruiseSpeed,
            final double wingArea, final double wingSpan, final double dragCoefficient,
            final double liftCoefficient, final double maximumRange,
            final Set<EngineModel> certifiedEngines) {
        final Maker maker = resolveUniqueModel(modelName, makerName);
        final AircraftModelName nameVO = AircraftModelName.valueOf(modelName);
        final AircraftModel model = new AircraftModel(nameVO, maker, type, motorizationType,
                emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                wingArea, wingSpan, dragCoefficient, liftCoefficient, maximumRange, certifiedEngines);
        return aircraftModelRepository.save(model);
    }

    /**
     * Legacy registration without an explicit wing span (the domain derives the
     * span from a nominal aspect ratio). Kept so existing web/registration callers
     * compile unchanged; new console paths use the full overload above.
     */
    public AircraftModel register(final String modelName, final String makerName,
            final AircraftType type, final MotorizationType motorizationType,
            final double emptyWeight, final double mtow, final double mzfw,
            final double maxFuelCapacity, final double serviceCeiling, final double cruiseSpeed,
            final double wingArea, final double dragCoefficient, final double liftCoefficient,
            final double maximumRange, final Set<EngineModel> certifiedEngines) {
        final Maker maker = resolveUniqueModel(modelName, makerName);
        final AircraftModelName nameVO = AircraftModelName.valueOf(modelName);
        final AircraftModel model = new AircraftModel(nameVO, maker, type, motorizationType,
                emptyWeight, mtow, mzfw, maxFuelCapacity, serviceCeiling, cruiseSpeed,
                wingArea, dragCoefficient, liftCoefficient, maximumRange, certifiedEngines);
        return aircraftModelRepository.save(model);
    }

    /** Resolves the maker and enforces the unique {@code (model name, maker)} rule. */
    private Maker resolveUniqueModel(final String modelName, final String makerName) {
        final MakerName makerNameVO = MakerName.valueOf(makerName);
        final Maker maker = makerRepository.findByName(makerNameVO)
                .orElseThrow(() -> new IllegalArgumentException("Maker not found: " + makerName));
        final AircraftModelName nameVO = AircraftModelName.valueOf(modelName);
        if (aircraftModelRepository.existsByNameAndMaker(nameVO, maker)) {
            throw new IntegrityViolationException(
                "An aircraft model with name '" + modelName + "' already exists for maker '" + makerName + "'");
        }
        return maker;
    }

    public Iterable<Maker> listMakers() {
        return makerRepository.findAll();
    }

    public Iterable<EngineModel> listEnginesByMotorizationType(final MotorizationType motorizationType) {
        return engineModelRepository.findByMotorizationType(motorizationType);
    }
}
