package eapli.aisafe.weather.application;

import java.time.LocalDateTime;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class RegisterWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final WeatherDataRepository weatherDataRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public RegisterWeatherDataController(final WeatherDataRepository weatherDataRepository,
            final AirControlAreaRepository airControlAreaRepository) {
        Preconditions.noneNull(weatherDataRepository, airControlAreaRepository);
        this.weatherDataRepository = weatherDataRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public Iterable<AirControlArea> listAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.WEATHER_PERSON);
        return airControlAreaRepository.findAll();
    }

    public WeatherData registerWeatherData(final String areaCodeValue,
            final LocalDateTime startTime, final LocalDateTime endTime,
            final double zoneSwLat, final double zoneSwLon,
            final double zoneNeLat, final double zoneNeLon,
            final double altLowerFeet, final double altUpperFeet,
            final double temperature, final double pressure, final double visibility,
            final double windSpeed, final double windDirection, final String dataSource) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.WEATHER_PERSON);

        final AreaCode areaCode = AreaCode.valueOf(areaCodeValue);
        airControlAreaRepository.findByCode(areaCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area not found: " + areaCodeValue));

        final WeatherZone zone = new WeatherZone(
                new Coordinates(zoneSwLat, zoneSwLon),
                new Coordinates(zoneNeLat, zoneNeLon),
                new AltitudeBand(altLowerFeet, altUpperFeet));

        final WeatherData data = new WeatherData(
                areaCode,
                new TimePeriod(startTime, endTime),
                zone,
                temperature, pressure, visibility,
                new WeatherWindSpeed(windSpeed),
                new WeatherWindDirection(windDirection),
                new DataSource(dataSource));
        return weatherDataRepository.save(data);
    }
}
