package eapli.aisafe.flightmanagement.domain;

import java.time.LocalDateTime;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.framework.validations.Preconditions;

/**
 * Domain Service / Factory for US080. Given the already-resolved aggregates it
 * validates the cross-aggregate creation <em>preconditions</em> that no single
 * aggregate owns and, when they hold, assembles a {@link Flight} with its initial
 * {@code DRAFT} plan.
 *
 * <p>Preconditions checked here (all raise {@link IllegalArgumentException}):</p>
 * <ul>
 *   <li>I2 — the pilot belongs to the route's owning company.</li>
 *   <li>I3 — the aircraft belongs to the route's company and is not decommissioned.</li>
 *   <li>I5 — the pilot is certified for the aircraft's model.</li>
 *   <li>I6 — the route is active (not deactivated) as of the departure date.</li>
 * </ul>
 *
 * <p>I1 (initial {@code DRAFT} status) is an initialization default owned by
 * {@link Flight}/{@link FlightPlan}; I4 (one flight per route per day) is a
 * uniqueness query owned by the application layer. This service never persists.</p>
 */
public class FlightCreationService {

    public Flight createDraft(final FlightRoute route, final Pilot pilot, final Aircraft aircraft,
            final AircraftModel aircraftModel, final LocalDateTime departure) {
        Preconditions.noneNull(route, pilot, aircraft, aircraftModel, departure);

        final CompanyIATACode routeCompany = route.company().iataCode();

        // I6 — route active as of the departure date. A route deactivated from a cutoff date
        // still accepts flights departing strictly before it (US074.4); a departure on or after
        // the cutoff is rejected (US074.3). deactivatedFrom() is non-null whenever the route is inactive.
        if (!route.isActive() && !departure.toLocalDate().isBefore(route.deactivatedFrom())) {
            throw new IllegalArgumentException(
                    "Route " + route.routeName() + " is deactivated as of " + route.deactivatedFrom());
        }
        // I2 — pilot belongs to the route's owning company
        if (!pilot.companyCode().equals(routeCompany)) {
            throw new IllegalArgumentException(
                    "Pilot does not belong to the route's owning company (" + routeCompany + ")");
        }
        // I3 — aircraft belongs to the route's company and is not decommissioned
        if (!aircraft.companyCode().equals(routeCompany)) {
            throw new IllegalArgumentException(
                    "Aircraft does not belong to the route's owning company (" + routeCompany + ")");
        }
        if (aircraft.status() != OperationalStatus.ACTIVE) {
            throw new IllegalArgumentException("Aircraft " + aircraft.registration() + " is decommissioned");
        }
        // I5 — pilot certified for the aircraft's model
        if (pilot.certifications().stream().noneMatch(aircraftModel::sameAs)) {
            throw new IllegalArgumentException(
                    "Pilot is not certified for the aircraft's model (" + aircraftModel.modelName() + ")");
        }

        return new Flight(route.routeName(), pilot.identity(), aircraft.registration(), departure);
    }
}