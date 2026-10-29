package eapli.aisafe.weather.application;

import java.time.LocalDate;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class ConsultWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final WeatherDataRepository weatherDataRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public ConsultWeatherDataController(final WeatherDataRepository weatherDataRepository,
            final AirControlAreaRepository airControlAreaRepository) {
        Preconditions.noneNull(weatherDataRepository, airControlAreaRepository);
        this.weatherDataRepository = weatherDataRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public Iterable<AirControlArea> listAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(
                AisafeRoles.WEATHER_PERSON, AisafeRoles.PILOT, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
        return airControlAreaRepository.findAll();
    }

    public Iterable<WeatherData> consultWeatherData(final String areaCodeValue, final LocalDate day) {
        authz.ensureAuthenticatedUserHasAnyOf(
                AisafeRoles.WEATHER_PERSON, AisafeRoles.PILOT, AisafeRoles.FLIGHT_CONTROL_OPERATOR);

        final AreaCode areaCode = AreaCode.valueOf(areaCodeValue);
        airControlAreaRepository.findByCode(areaCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area not found: " + areaCodeValue));

        return weatherDataRepository.findByAreaCodeAndDay(areaCode, day);
    }
}
