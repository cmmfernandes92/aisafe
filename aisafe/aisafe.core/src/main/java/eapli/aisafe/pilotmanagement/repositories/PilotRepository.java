package eapli.aisafe.pilotmanagement.repositories;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.repositories.DomainRepository;

public interface PilotRepository extends DomainRepository<Email, Pilot> {

    Iterable<Pilot> findAllByCompanyCode(CompanyIATACode companyCode);

    Iterable<Pilot> findAllActivePilotsByCompanyCode(CompanyIATACode companyCode);

    Iterable<Pilot> findAllActivePilots();
}
