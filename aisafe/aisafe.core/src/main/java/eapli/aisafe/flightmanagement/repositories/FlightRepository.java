package eapli.aisafe.flightmanagement.repositories;

import java.time.LocalDate;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.repositories.DomainRepository;

public interface FlightRepository extends DomainRepository<FlightId, Flight> {

    /**
     * Whether a flight already exists for the given route on the given date.
     * Backs the "one flight per route per day" uniqueness check (US080 / I4).
     */
    boolean existsForRouteOnDate(RouteName route, LocalDate date);

    /**
     * The flights owned by the given pilot. Backs US121's "import a plan for one
     * of <em>my own</em> flights" selection: the pilot chooses a target flight
     * instead of the system deriving it from the file's designator.
     */
    Iterable<Flight> flightsOf(Email pilot);

    /**
     * US074 (AC2): whether any flight on the route departs on or after the date.
     */
    boolean existsForRouteOnOrAfter(RouteName route, LocalDate date);
}