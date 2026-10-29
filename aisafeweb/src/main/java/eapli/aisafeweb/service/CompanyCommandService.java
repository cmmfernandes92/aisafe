package eapli.aisafeweb.service;

import org.springframework.stereotype.Service;

import eapli.aisafe.airtransportcompanymanagement.application.RegisterAirTransportCompanyController;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;

@Service
public class CompanyCommandService {

    public void register(final String iataCode, final String icaoCode, final String companyName) {
        final var controller = new RegisterAirTransportCompanyController(
                PersistenceContext.repositories().airTransportCompanies());
        controller.register(iataCode, icaoCode, companyName);
    }
}
