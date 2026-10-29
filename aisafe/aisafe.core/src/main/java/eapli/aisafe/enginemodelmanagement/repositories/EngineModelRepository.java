package eapli.aisafe.enginemodelmanagement.repositories;

import java.util.Optional;

import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.repositories.DomainRepository;

public interface EngineModelRepository extends DomainRepository<Long, EngineModel> {

    Optional<EngineModel> findByNameAndMaker(EngineModelName name, Maker maker);

    boolean existsByNameAndMaker(EngineModelName name, Maker maker);

    Iterable<EngineModel> findByMotorizationType(MotorizationType motorizationType);
}
