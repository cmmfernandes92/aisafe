package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;

import eapli.aisafe.Application;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaAircraftRepository
        extends JpaAutoTxRepository<Aircraft, Registration, Registration>
        implements AircraftRepository {

    public JpaAircraftRepository(final TransactionalContext autoTx) {
        super(autoTx, "registration");
    }

    public JpaAircraftRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "registration");
    }

    @Override
    public Iterable<Aircraft> findAllByCompany(final CompanyIATACode companyCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyCode", companyCode);
        return match("e.companyCode = :companyCode", params);
    }
}
