package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryPilotRepository extends InMemoryDomainRepository<Pilot, Email> implements PilotRepository {

    @Override
    public Iterable<Pilot> findAllByCompanyCode(final CompanyIATACode companyCode) {
        return match(e -> e.companyCode().equals(companyCode));
    }

    @Override
    public Iterable<Pilot> findAllActivePilotsByCompanyCode(final CompanyIATACode companyCode) {
        return match(e -> e.companyCode().equals(companyCode) && e.isActive());
    }

    @Override
    public Iterable<Pilot> findAllActivePilots() {
        return match(Pilot::isActive);
    }
}
