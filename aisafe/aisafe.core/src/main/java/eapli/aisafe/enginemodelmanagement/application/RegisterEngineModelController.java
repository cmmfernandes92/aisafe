package eapli.aisafe.enginemodelmanagement.application;

import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;

@UseCaseController
public class RegisterEngineModelController {

    private final EngineModelRepository engineModelRepository;
    private final MakerRepository makerRepository;

    public RegisterEngineModelController(final EngineModelRepository engineModelRepository,
            final MakerRepository makerRepository) {
        this.engineModelRepository = engineModelRepository;
        this.makerRepository = makerRepository;
    }

    /**
     * Full registration (US085): includes the {@code cruiseThrust} (second §3.3
     * thrust point) so the engine can drive the flight-plan physics test.
     */
    public EngineModel register(final String engineModelName, final String makerName,
            final MotorizationType motorizationType, final FuelType fuelType,
            final double power, final double cruiseThrust, final double efficiency) {
        final Maker maker = resolveUniqueEngine(engineModelName, makerName);
        final EngineModelName nameVO = EngineModelName.valueOf(engineModelName);
        final EngineModel model = new EngineModel(nameVO, maker, motorizationType, fuelType,
                power, cruiseThrust, efficiency);
        return engineModelRepository.save(model);
    }

    /**
     * Legacy registration without an explicit cruise-thrust point (the domain
     * derives it from a nominal fraction of static thrust). Kept so existing
     * web/registration callers compile unchanged; new console paths use the full
     * overload above.
     */
    public EngineModel register(final String engineModelName, final String makerName,
            final MotorizationType motorizationType, final FuelType fuelType,
            final double power, final double efficiency) {
        final Maker maker = resolveUniqueEngine(engineModelName, makerName);
        final EngineModelName nameVO = EngineModelName.valueOf(engineModelName);
        final EngineModel model = new EngineModel(nameVO, maker, motorizationType, fuelType, power, efficiency);
        return engineModelRepository.save(model);
    }

    /** Resolves the maker and enforces the unique {@code (engine name, maker)} rule. */
    private Maker resolveUniqueEngine(final String engineModelName, final String makerName) {
        final MakerName makerNameVO = MakerName.valueOf(makerName);
        final Maker maker = makerRepository.findByName(makerNameVO)
                .orElseThrow(() -> new IllegalArgumentException("Maker not found: " + makerName));
        final EngineModelName nameVO = EngineModelName.valueOf(engineModelName);
        if (engineModelRepository.existsByNameAndMaker(nameVO, maker)) {
            throw new IntegrityViolationException(
                "An engine model with name '" + engineModelName + "' already exists for maker '" + makerName + "'");
        }
        return maker;
    }

    public Iterable<Maker> listMakers() {
        return makerRepository.findAll();
    }
}
