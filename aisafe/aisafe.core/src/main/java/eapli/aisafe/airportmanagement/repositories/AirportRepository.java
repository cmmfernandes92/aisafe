package eapli.aisafe.airportmanagement.repositories;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.framework.domain.repositories.DomainRepository;

public interface AirportRepository extends DomainRepository<AirportICAOCode, Airport> {

    Optional<Airport> findByIcaoCode(AirportICAOCode icaoCode);

    Optional<Airport> findByIataCode(AirportIATACode iataCode);

    Iterable<Airport> findByAreaCode(AreaCode areaCode);

    Iterable<Airport> findByCountryCode(CountryCode countryCode);
}
