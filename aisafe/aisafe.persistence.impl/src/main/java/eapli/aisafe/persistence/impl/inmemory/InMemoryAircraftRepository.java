package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, Registration>
        implements AircraftRepository {

    @Override
    public Iterable<Aircraft> findAllByCompany(final CompanyIATACode companyCode) {
        return match(a -> a.companyCode().equals(companyCode));
    }
}
