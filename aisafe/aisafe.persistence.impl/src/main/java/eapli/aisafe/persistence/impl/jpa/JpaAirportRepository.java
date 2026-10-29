package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaAirportRepository
        extends JpaAutoTxRepository<Airport, AirportICAOCode, AirportICAOCode>
        implements AirportRepository {

    public JpaAirportRepository(final TransactionalContext autoTx) {
        super(autoTx, "icaoCode");
    }

    public JpaAirportRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "icaoCode");
    }

    @Override
    public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("icaoCode", icaoCode);
        return matchOne("e.icaoCode = :icaoCode", params);
    }

    @Override
    public Optional<Airport> findByIataCode(final AirportIATACode iataCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iataCode", iataCode);
        return matchOne("e.iataCode = :iataCode", params);
    }

    @Override
    public Iterable<Airport> findByAreaCode(final AreaCode areaCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode);
        return match("e.areaCode = :areaCode", params);
    }

    @Override
    public Iterable<Airport> findByCountryCode(final CountryCode countryCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("countryCode", countryCode);
        return match("e.countryCode = :countryCode", params);
    }
}
