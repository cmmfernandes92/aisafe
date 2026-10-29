package eapli.aisafeweb.service;

import org.springframework.stereotype.Service;

import eapli.aisafe.aircontrolareamanagement.application.RegisterAirControlAreaController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class AirControlAreaCommandService {

    public void register(
            final String code,
            final String name,
            final double swLat,
            final double swLon,
            final double neLat,
            final double neLon) {
        final var controller = new RegisterAirControlAreaController(
                PersistenceContext.repositories().airControlAreas());
        controller.registerArea(code, name, swLat, swLon, neLat, neLon);
    }
}
