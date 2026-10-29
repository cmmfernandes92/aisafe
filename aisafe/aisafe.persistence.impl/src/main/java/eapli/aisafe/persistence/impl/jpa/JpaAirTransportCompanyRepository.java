package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaAirTransportCompanyRepository
        extends JpaAutoTxRepository<AirTransportCompany, CompanyIATACode, CompanyIATACode>
        implements AirTransportCompanyRepository {

    public JpaAirTransportCompanyRepository(final TransactionalContext autoTx) {
        super(autoTx, "iataCode");
    }

    public JpaAirTransportCompanyRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "iataCode");
    }

    @Override
    public Optional<AirTransportCompany> findByIataCode(final CompanyIATACode iataCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iataCode", iataCode);
        return matchOne("e.iataCode=:iataCode", params);
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final CompanyICAOCode icaoCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("icaoCode", icaoCode);
        return matchOne("e.icaoCode=:icaoCode", params);
    }
}
