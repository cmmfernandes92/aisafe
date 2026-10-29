package eapli.aisafe.flightmanagement.application;

import java.util.Map;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class AttachWeatherToPlanController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final FlightRepository flightRepository =
            PersistenceContext.repositories().flights();

    private final WeatherDataRepository weatherRepository =
            PersistenceContext.repositories().weatherData();

    public void attachWeather(final FlightId flightId,
                              final Map<SegmentKey, WeatherDataId> weatherMap) {

        // 1. garantir que é piloto
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.PILOT);

        // 2. carregar flight
        final Flight flight = flightRepository.ofIdentity(flightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found"));

        // ✅ ownership check (I1)
        final var user = authz.session()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"))
                .authenticatedUser();

        final String email = user.email().toString();

        if (!flight.pilot().toString().equals(email)) {
            throw new IllegalStateException("Pilot does not own the flight");
        }

        // 4. validar que weather existe
        for (WeatherDataId weatherId : weatherMap.values()) {

            if (weatherRepository.ofIdentity(weatherId).isEmpty()) {
                throw new IllegalArgumentException("Unknown weather data: " + weatherId);
            }
        }

        // 5. chamar domínio
        flight.attachWeatherToCurrentPlan(weatherMap);

        // 6. guardar
        flightRepository.save(flight);
    }
}