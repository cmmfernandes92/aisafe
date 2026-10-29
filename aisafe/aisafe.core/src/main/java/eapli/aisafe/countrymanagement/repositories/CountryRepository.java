package eapli.aisafe.countrymanagement.repositories;

import java.util.Optional;

import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.countrymanagement.domain.CountryName;
import eapli.framework.domain.repositories.DomainRepository;

public interface CountryRepository extends DomainRepository<CountryCode, Country> {

    Optional<Country> findByCode(CountryCode code);

    Optional<Country> findByName(CountryName name);
}
