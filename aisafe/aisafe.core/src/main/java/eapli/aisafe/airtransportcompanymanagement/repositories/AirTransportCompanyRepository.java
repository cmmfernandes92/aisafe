package eapli.aisafe.airtransportcompanymanagement.repositories;

import java.util.Optional;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.framework.domain.repositories.DomainRepository;

public interface AirTransportCompanyRepository extends DomainRepository<CompanyIATACode, AirTransportCompany> {

    Optional<AirTransportCompany> findByIataCode(CompanyIATACode iataCode);

    Optional<AirTransportCompany> findByIcaoCode(CompanyICAOCode icaoCode);
}
