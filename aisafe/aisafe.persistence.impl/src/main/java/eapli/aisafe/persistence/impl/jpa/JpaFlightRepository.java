package eapli.aisafe.persistence.impl.jpa;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import eapli.aisafe.Application;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaFlightRepository
        extends JpaAutoTxRepository<Flight, FlightId, FlightId>
        implements FlightRepository {

    public JpaFlightRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaFlightRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
        return ofIdentity(new FlightId(route, date)).isPresent();
    }

    @Override
    public Iterable<Flight> flightsOf(final Email pilot) {
        return match("e.pilot.email = :email", Map.of("email", pilot.toString()));
    }

    @Override
    public boolean existsForRouteOnOrAfter(final RouteName route, final LocalDate date) {
        final Map<String, Object> params = new HashMap<>();
        params.put("route", route);
        params.put("date", date);
        return match("e.id.routeName = :route AND e.id.departureDate >= :date", params)
                .iterator().hasNext();
    }
}