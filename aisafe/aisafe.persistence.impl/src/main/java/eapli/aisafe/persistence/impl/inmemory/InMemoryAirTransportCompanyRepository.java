package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAirTransportCompanyRepository
        extends InMemoryDomainRepository<AirTransportCompany, CompanyIATACode>
        implements AirTransportCompanyRepository {

    @Override
    public Optional<AirTransportCompany> findByIataCode(final CompanyIATACode iataCode) {
        return matchOne(e -> e.iataCode().equals(iataCode));
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final CompanyICAOCode icaoCode) {
        return matchOne(e -> e.icaoCode().equals(icaoCode));
    }
}
