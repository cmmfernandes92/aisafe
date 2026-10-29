package eapli.aisafe.airtransportcompanymanagement.application;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.IntegrityViolationException;

@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AirTransportCompanyRepository repository;

    public RegisterAirTransportCompanyController(final AirTransportCompanyRepository repository) {
        this.repository = repository;
    }

    public AirTransportCompany register(final String iataCode, final String icaoCode,
            final String companyName) {
        final CompanyIATACode iata = CompanyIATACode.valueOf(iataCode);
        final CompanyICAOCode icao = CompanyICAOCode.valueOf(icaoCode);

        if (repository.findByIataCode(iata).isPresent()) {
            throw new IntegrityViolationException("IATA code '" + iataCode + "' is already registered");
        }
        if (repository.findByIcaoCode(icao).isPresent()) {
            throw new IntegrityViolationException("ICAO code '" + icaoCode + "' is already registered");
        }

        final AirTransportCompany company = new AirTransportCompany(iata, icao, companyName);
        return repository.save(company);
    }
}
