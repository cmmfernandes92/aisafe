package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryEngineModelRepository extends InMemoryDomainRepository<EngineModel, Long>
        implements EngineModelRepository {

    @Override
    public Optional<EngineModel> findByNameAndMaker(final EngineModelName name, final Maker maker) {
        return matchOne(e -> e.engineModelName().equals(name) && e.maker().equals(maker));
    }

    @Override
    public boolean existsByNameAndMaker(final EngineModelName name, final Maker maker) {
        return findByNameAndMaker(name, maker).isPresent();
    }

    @Override
    public Iterable<EngineModel> findByMotorizationType(final MotorizationType motorizationType) {
        return match(e -> e.motorizationType() == motorizationType);
    }
}
