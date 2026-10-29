package eapli.aisafe.persistence.impl.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eapli.aisafe.pilotmanagement.domain.PilotStatus;

import eapli.aisafe.Application;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaPilotRepository extends JpaAutoTxRepository<Pilot, Email, Email> implements PilotRepository {

    public JpaPilotRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "email");
    }

    @Override
    public Iterable<Pilot> findAllByCompanyCode(final CompanyIATACode companyCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("companyCode", companyCode);
        return match("e.companyCode = :companyCode", params);
    }

    @Override
    public Iterable<Pilot> findAllActivePilotsByCompanyCode(final CompanyIATACode companyCode) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : findAllByCompanyCode(companyCode)) {
            if (p.isActive()) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public Iterable<Pilot> findAllActivePilots() {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", PilotStatus.ACTIVE);
        return match("e.status = :status", params);
    }
}
