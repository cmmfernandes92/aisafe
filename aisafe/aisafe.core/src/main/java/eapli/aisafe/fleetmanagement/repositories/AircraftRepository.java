package eapli.aisafe.fleetmanagement.repositories;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.framework.domain.repositories.DomainRepository;

public interface AircraftRepository extends DomainRepository<Registration, Aircraft> {

    Iterable<Aircraft> findAllByCompany(CompanyIATACode companyCode);
}
