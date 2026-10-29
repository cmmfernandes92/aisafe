package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAirportRepository
        extends InMemoryDomainRepository<Airport, AirportICAOCode>
        implements AirportRepository {

    @Override
    public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
        return matchOne(e -> e.icaoCode().equals(icaoCode));
    }

    @Override
    public Optional<Airport> findByIataCode(final AirportIATACode iataCode) {
        return matchOne(e -> e.iataCode().equals(iataCode));
    }

    @Override
    public Iterable<Airport> findByAreaCode(final AreaCode areaCode) {
        return match(e -> e.areaCode().equals(areaCode));
    }

    @Override
    public Iterable<Airport> findByCountryCode(final CountryCode countryCode) {
        return match(e -> e.countryCode().equals(countryCode));
    }
}
