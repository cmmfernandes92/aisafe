package eapli.aisafe.flightroutemanagement.repositories;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.framework.domain.repositories.DomainRepository;

public interface FlightRouteRepository extends DomainRepository<RouteName, FlightRoute> {

    boolean existsByName(RouteName routeName);

    Iterable<FlightRoute> findByCompany(AirTransportCompany company);

    // US074: active routes of a company for the deactivation UI
    Iterable<FlightRoute> findActiveByCompany(AirTransportCompany company);
}
