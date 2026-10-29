package eapli.aisafe.flightmanagement.application;

import java.time.LocalDateTime;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightCreationService;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.FlightRoute;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.flightroutemanagement.repositories.FlightRouteRepository;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;

/**
 * US080 - creates a {@link Flight} for a route with its initial {@code DRAFT}
 * flight plan. GRASP <em>Controller</em>: it authenticates the Pilot, resolves the
 * referenced aggregates via their repositories, runs the route+date uniqueness
 * query (I4), then delegates the cross-aggregate decision to the
 * {@link FlightCreationService}. It holds no business rule.
 *
 * <p>The flight's pilot is the authenticated user; it is resolved through the
 * application user profile (session {@code SystemUser} -&gt; {@link Email} -&gt;
 * {@link Pilot}).</p>
 */
@UseCaseController
public class CreateFlightController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightCreationService flightCreationService = new FlightCreationService();

    private final FlightRepository flightRepository;
    private final FlightRouteRepository flightRouteRepository;
    private final AircraftRepository aircraftRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final PilotRepository pilotRepository;
    private final UserRepository userRepository;

    public CreateFlightController(final FlightRepository flightRepository,
            final FlightRouteRepository flightRouteRepository, final AircraftRepository aircraftRepository,
            final AircraftModelRepository aircraftModelRepository, final PilotRepository pilotRepository,
            final UserRepository userRepository) {
        Preconditions.noneNull(flightRepository, flightRouteRepository, aircraftRepository,
                aircraftModelRepository, pilotRepository, userRepository);
        this.flightRepository = flightRepository;
        this.flightRouteRepository = flightRouteRepository;
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.pilotRepository = pilotRepository;
        this.userRepository = userRepository;
    }

    public Flight createDraftFlight(final RouteName routeName, final LocalDateTime departure,
            final Registration registration) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.PILOT);
        Preconditions.noneNull(routeName, departure, registration);

        final FlightRoute route = flightRouteRepository.ofIdentity(routeName)
                .orElseThrow(() -> new IllegalArgumentException("Flight route not found: " + routeName));
        final Aircraft aircraft = aircraftRepository.ofIdentity(registration)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + registration));
        final AircraftModel aircraftModel = aircraftModelRepository.ofIdentity(aircraft.modelId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aircraft model not found: " + aircraft.modelId()));
        final Pilot pilot = authenticatedPilot();

        if (flightRepository.existsForRouteOnDate(routeName, departure.toLocalDate())) {
            throw new IllegalArgumentException(
                    "A flight already exists for route " + routeName + " on " + departure.toLocalDate());
        }

        final Flight flight = flightCreationService.createDraft(route, pilot, aircraft, aircraftModel, departure);
        return flightRepository.save(flight);
    }

    private Pilot authenticatedPilot() {
        final SystemUser systemUser = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session"))
                .authenticatedUser();
        final Email email = userRepository.findBySystemUser(systemUser)
                .orElseThrow(() -> new IllegalStateException("No user profile found for the authenticated user"))
                .identity();
        return pilotRepository.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException("No pilot profile found for " + email));
    }
}