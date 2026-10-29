package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaAirControlAreaRepository
		extends JpaAutoTxRepository<AirControlArea, AreaCode, AreaCode>
		implements AirControlAreaRepository {

	public JpaAirControlAreaRepository(final TransactionalContext autoTx) {
		super(autoTx, "code");
	}

	public JpaAirControlAreaRepository(final String puname) {
		super(puname, Application.settings().getExtendedPersistenceProperties(), "code");
	}

	@Override
	public Optional<AirControlArea> findByCode(final AreaCode code) {
		final Map<String, Object> params = new HashMap<>();
		params.put("code", code);
		return matchOne("e.code = :code", params);
	}

	@Override
	public Optional<AirControlArea> findByName(final AreaName name) {
		final Map<String, Object> params = new HashMap<>();
		params.put("name", name);
		return matchOne("e.name = :name", params);
	}

	@Override
	public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
		final Map<String, Object> params = new HashMap<>();
		params.put("swLat", boundary.southwest().latitude());
		params.put("swLon", boundary.southwest().longitude());
		params.put("neLat", boundary.northeast().latitude());
		params.put("neLon", boundary.northeast().longitude());
		return match(
				"e.boundary.northeast.longitude >= :swLon"
				+ " AND e.boundary.southwest.longitude <= :neLon"
				+ " AND e.boundary.northeast.latitude >= :swLat"
				+ " AND e.boundary.southwest.latitude <= :neLat",
				params);
	}
}