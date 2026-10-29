package eapli.aisafe.airportmanagement.application;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.domain.AirportName;
import eapli.aisafe.airportmanagement.domain.Elevation;
import eapli.aisafe.airportmanagement.domain.Town;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class RegisterAirportController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirportRepository airportRepository;
    private final AirControlAreaRepository airControlAreaRepository;
    private final CountryRepository countryRepository;

    public RegisterAirportController(final AirportRepository airportRepository,
            final AirControlAreaRepository airControlAreaRepository,
            final CountryRepository countryRepository) {
        Preconditions.noneNull(airportRepository, airControlAreaRepository, countryRepository);
        this.airportRepository = airportRepository;
        this.airControlAreaRepository = airControlAreaRepository;
        this.countryRepository = countryRepository;
    }

    public Airport registerAirport(final String icaoCode, final String iataCode,
            final String airportName, final String town, final double elevationMeters,
            final double latitude, final double longitude, final String countryCode,
            final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.BACKOFFICE_OPERATOR);

        final AirportICAOCode airportICAOCode = new AirportICAOCode(icaoCode);
        final AirportIATACode airportIATACode = new AirportIATACode(iataCode);
        final AirportName name = new AirportName(airportName);
        final Town airportTown = new Town(town);
        final Elevation elevation = new Elevation(elevationMeters);
        final Coordinates coordinates = new Coordinates(latitude, longitude);
        final CountryCode country = new CountryCode(countryCode);
        final AreaCode area = new AreaCode(areaCode);

        if (airportRepository.findByIcaoCode(airportICAOCode).isPresent()) {
            throw new IllegalArgumentException("Airport ICAO code already registered: " + icaoCode);
        }

        if (airportRepository.findByIataCode(airportIATACode).isPresent()) {
            throw new IllegalArgumentException("Airport IATA code already registered: " + iataCode);
        }

        if (countryRepository.findByCode(country).isEmpty()) {
            throw new IllegalArgumentException("Country is not registered: " + countryCode);
        }

        final AirControlArea airControlArea = airControlAreaRepository.findByCode(area)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area is not registered: " + areaCode));

        if (!airControlArea.boundary().contains(coordinates)) {
            throw new IllegalArgumentException(
                    "Airport coordinates are outside the selected air control area");
        }

        final Airport airport = new Airport(airportICAOCode, airportIATACode, name, airportTown,
                elevation, coordinates, country, area);
        return airportRepository.save(airport);
    }
}
