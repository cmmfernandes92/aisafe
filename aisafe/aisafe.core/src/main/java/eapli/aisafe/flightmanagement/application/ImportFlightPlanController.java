package eapli.aisafe.flightmanagement.application;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;

/**
 * US121 - imports a validated flight-plan DSL body into an <em>existing</em>
 * flight owned by the authenticated pilot, and promotes its draft
 * {@code DRAFT -> VALIDATED}.
 *
 * <p>The flight must already exist (created by US080); this controller does not
 * create one. The pilot picks the target flight from {@link
 * #flightsOfAuthenticatedPilot()} rather than the system deriving it from the
 * file's designator. The file's lexical/syntactic/semantic validation (US120) is
 * run by the calling adapter before {@link #importFlightPlan} is reached, so the
 * controller stays DSL-unaware: it receives only the chosen {@link FlightId} and
 * the validated raw content. This keeps the controller the single reusable
 * indirection point for the use case (a future TCP adapter for US086 calls the
 * same method).</p>
 */
@UseCaseController
public class ImportFlightPlanController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRepository flightRepository;

    public ImportFlightPlanController(final FlightRepository flightRepository) {
        Preconditions.nonNull(flightRepository);
        this.flightRepository = flightRepository;
    }

    /** The flights owned by the authenticated pilot, to choose an import target from. */
    public Iterable<Flight> flightsOfAuthenticatedPilot() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.PILOT);
        return flightRepository.flightsOf(authenticatedPilotEmail());
    }

    public Flight importFlightPlan(final FlightId flightId, final String rawDslContent) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.PILOT);
        Preconditions.nonNull(flightId);
        Preconditions.nonEmpty(rawDslContent, "Flight plan content must not be empty");

        final Flight flight = flightRepository.ofIdentity(flightId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No flight exists for " + flightId + "; it must be created first (US080)."));

        ensureOwnedByAuthenticatedPilot(flight);

        flight.importDsl(rawDslContent, authenticatedUser().identity().toString());
        return flightRepository.save(flight);
    }

    private void ensureOwnedByAuthenticatedPilot(final Flight flight) {
        if (!flight.pilot().equals(authenticatedPilotEmail())) {
            throw new IllegalArgumentException(
                    "This flight belongs to another pilot; you can only import plans for your own flights.");
        }
    }

    private Email authenticatedPilotEmail() {
        return Email.valueOf(authenticatedUser().email().toString());
    }

    private SystemUser authenticatedUser() {
        return authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session"))
                .authenticatedUser();
    }
}