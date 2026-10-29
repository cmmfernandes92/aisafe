package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAirControlAreaRepository
		extends InMemoryDomainRepository<AirControlArea, AreaCode>
		implements AirControlAreaRepository {

	@Override
	public Optional<AirControlArea> findByCode(final AreaCode code) {
		return matchOne(e -> e.code().equals(code));
	}

	@Override
	public Optional<AirControlArea> findByName(final AreaName name) {
		return matchOne(e -> e.name().equals(name));
	}

	@Override
	public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
		return match(e -> e.boundary().overlaps(boundary));
	}
}