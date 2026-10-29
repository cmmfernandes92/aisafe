package eapli.aisafeweb.service;

import org.springframework.stereotype.Service;

import eapli.aisafe.airportmanagement.application.RegisterAirportController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class AirportCommandService {

    public void register(
            final String icaoCode,
            final String iataCode,
            final String airportName,
            final String town,
            final double elevationMeters,
            final double latitude,
            final double longitude,
            final String countryCode,
            final String areaCode) {
        final var controller = new RegisterAirportController(
                PersistenceContext.repositories().airports(),
                PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().countries());
        controller.registerAirport(icaoCode, iataCode, airportName, town, elevationMeters,
                latitude, longitude, countryCode, areaCode);
    }
}
