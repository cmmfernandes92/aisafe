package eapli.aisafe.flightmanagement.application;

import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestService;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;

/**
 * US085 – tests/validates the current plan of a flight owned by the
 * authenticated pilot. On success the plan is promoted
 * {@code VALIDATED -> TESTED}; on failure the plan state is unchanged.
 *
 * <p>The caller supplies the {@link FlightPlanTestService} so that the
 * controller stays decoupled from the simulation strategy (GRASP Indirection /
 * Strategy). The production implementation is
 * {@link SimulatorV1FlightPlanTestService} (the §3.3 Simulator&nbsp;v1 model).</p>
 */
@UseCaseController
public class TestFlightPlanController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRepository flightRepository;
    private final FlightPlanTestService testService;

    public TestFlightPlanController(final FlightRepository flightRepository,
            final FlightPlanTestService testService) {
        Preconditions.noneNull(flightRepository, testService);
        this.flightRepository = flightRepository;
        this.testService = testService;
    }

    public FlightPlanTestResult testFlightPlan(final FlightId flightId) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.PILOT);
        Preconditions.nonNull(flightId);

        final Flight flight = flightRepository.ofIdentity(flightId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No flight exists for " + flightId + "; it must be created first (US080)."));

        ensureOwnedByAuthenticatedPilot(flight);

        final FlightPlanTestResult result = flight.testCurrentPlan(testService);
        if (result.isSuccess()) {
            flightRepository.save(flight);
        }
        return result;
    }

    private void ensureOwnedByAuthenticatedPilot(final Flight flight) {
        if (!flight.pilot().equals(authenticatedPilotEmail())) {
            throw new IllegalArgumentException(
                    "This flight belongs to another pilot; you can only test plans for your own flights.");
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
