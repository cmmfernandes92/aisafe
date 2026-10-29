package eapli.aisafe.aircontrolareamanagement.application;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class RegisterAirControlAreaController {

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final AirControlAreaRepository repository;

	public RegisterAirControlAreaController(final AirControlAreaRepository repository) {
		if (repository == null) {
			throw new IllegalArgumentException("Repository must not be null");
		}
		this.repository = repository;
	}

	public AirControlArea registerArea(final String code, final String name,
			final double swLat, final double swLon, final double neLat, final double neLon) {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.BACKOFFICE_OPERATOR);

		final AreaCode areaCode = new AreaCode(code);
		final AreaName areaName = new AreaName(name);
		final Coordinates southwest = new Coordinates(swLat, swLon);
		final Coordinates northeast = new Coordinates(neLat, neLon);
		final GeographicBoundary boundary = new GeographicBoundary(southwest, northeast);

		if (repository.findByCode(areaCode).isPresent()) {
			throw new IllegalArgumentException("Area code already registered: " + code);
		}

		if (repository.findByName(areaName).isPresent()) {
			throw new IllegalArgumentException("Area name already registered: " + name);
		}

		final Iterable<AirControlArea> overlapping = repository.findOverlapping(boundary);
		if (overlapping.iterator().hasNext()) {
			throw new IllegalArgumentException("Boundary overlaps with an existing air control area");
		}

		final AirControlArea area = new AirControlArea(areaCode, areaName, boundary);
		return repository.save(area);
	}
}
