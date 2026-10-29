package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.countrymanagement.domain.CountryName;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryCountryRepository
        extends InMemoryDomainRepository<Country, CountryCode>
        implements CountryRepository {

    @Override
    public Optional<Country> findByCode(final CountryCode code) {
        return matchOne(e -> e.code().equals(code));
    }

    @Override
    public Optional<Country> findByName(final CountryName name) {
        return matchOne(e -> e.name().equals(name));
    }
}
