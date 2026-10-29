package eapli.aisafe.aircraftmodelmanagement.repositories;

import java.util.Optional;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.repositories.DomainRepository;

public interface AircraftModelRepository extends DomainRepository<Long, AircraftModel> {

    Optional<AircraftModel> findByNameAndMaker(AircraftModelName name, Maker maker);

    boolean existsByNameAndMaker(AircraftModelName name, Maker maker);
}
