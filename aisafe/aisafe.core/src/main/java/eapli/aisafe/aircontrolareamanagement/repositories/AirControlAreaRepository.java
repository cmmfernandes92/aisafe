package eapli.aisafe.aircontrolareamanagement.repositories;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.framework.domain.repositories.DomainRepository;

public interface AirControlAreaRepository extends DomainRepository<AreaCode, AirControlArea> {

	Optional<AirControlArea> findByCode(AreaCode code);

	Optional<AirControlArea> findByName(AreaName name);

	Iterable<AirControlArea> findOverlapping(GeographicBoundary boundary);
}