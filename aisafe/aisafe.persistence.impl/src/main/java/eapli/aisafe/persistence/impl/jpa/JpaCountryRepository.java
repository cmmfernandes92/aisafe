package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.countrymanagement.domain.CountryName;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaCountryRepository
        extends JpaAutoTxRepository<Country, CountryCode, CountryCode>
        implements CountryRepository {

    public JpaCountryRepository(final TransactionalContext autoTx) {
        super(autoTx, "code");
    }

    public JpaCountryRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "code");
    }

    @Override
    public Optional<Country> findByCode(final CountryCode code) {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        return matchOne("e.code = :code", params);
    }

    @Override
    public Optional<Country> findByName(final CountryName name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        return matchOne("e.name = :name", params);
    }
}
