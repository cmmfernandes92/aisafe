package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, RouteName>
        implements FlightRouteRepository {

    @Override
    public boolean existsByName(final RouteName routeName) {
        return matchOne(e -> e.routeName().equals(routeName)).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final AirTransportCompany company) {
        return match(e -> e.company().equals(company));
    }

    @Override
    public Iterable<FlightRoute> findActiveByCompany(final AirTransportCompany company) {
        return match(e -> e.company().equals(company) && e.isActive());
    }
}
