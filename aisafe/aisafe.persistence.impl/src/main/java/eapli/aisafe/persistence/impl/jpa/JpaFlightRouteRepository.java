package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;

import eapli.aisafe.Application;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaFlightRouteRepository
        extends JpaAutoTxRepository<FlightRoute, RouteName, RouteName>
        implements FlightRouteRepository {

    public JpaFlightRouteRepository(final TransactionalContext autoTx) {
        super(autoTx, "routeName");
    }

    public JpaFlightRouteRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "routeName");
    }

    @Override
    public boolean existsByName(final RouteName routeName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("routeName", routeName);
        return matchOne("e.routeName = :routeName", params).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("company", company);
        return match("e.company = :company", params);
    }

    @Override
    public Iterable<FlightRoute> findActiveByCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("company", company);
        return match("e.company = :company AND e.active = true", params);
    }
}
