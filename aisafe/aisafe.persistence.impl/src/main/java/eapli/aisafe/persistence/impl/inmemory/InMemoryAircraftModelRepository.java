package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAircraftModelRepository extends InMemoryDomainRepository<AircraftModel, Long>
        implements AircraftModelRepository {

    @Override
    public Optional<AircraftModel> findByNameAndMaker(final AircraftModelName name, final Maker maker) {
        return matchOne(e -> e.modelName().equals(name) && e.maker().equals(maker));
    }

    @Override
    public boolean existsByNameAndMaker(final AircraftModelName name, final Maker maker) {
        return findByNameAndMaker(name, maker).isPresent();
    }
}
