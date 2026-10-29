package eapli.aisafe.persistence.impl.inmemory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryFlightRepository
        extends InMemoryDomainRepository<Flight, FlightId>
        implements FlightRepository {

    @Override
    public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
        return ofIdentity(new FlightId(route, date)).isPresent();
    }

    @Override
    public Iterable<Flight> flightsOf(final Email pilot) {
        final List<Flight> result = new ArrayList<>();
        for (final Flight flight : findAll()) {
            if (flight.pilot().equals(pilot)) {
                result.add(flight);
            }
        }
        return result;
    }

    @Override
    public boolean existsForRouteOnOrAfter(final RouteName route, final LocalDate date) {
        return matchOne(f -> f.route().equals(route)
                && !f.departure().toLocalDate().isBefore(date)).isPresent();
    }
}