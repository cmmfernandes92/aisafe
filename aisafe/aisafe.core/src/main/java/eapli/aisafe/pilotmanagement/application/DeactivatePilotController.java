package eapli.aisafe.pilotmanagement.application;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class DeactivatePilotController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final PilotRepository pilotRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;

    public DeactivatePilotController(final PilotRepository pilotRepository,
            final FlightRepository flightRepository,
            final UserRepository userRepository,
            final CollaboratorRepository collaboratorRepository) {
        Preconditions.noneNull(pilotRepository, flightRepository, userRepository, collaboratorRepository);
        this.pilotRepository = pilotRepository;
        this.flightRepository = flightRepository;
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public Iterable<Pilot> listActivePilotsOfMyCompany() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AisafeRoles.POWER_USER);
        if (authz.isAuthenticatedUserAuthorizedTo(AisafeRoles.POWER_USER)) {
            return allActivePilots();
        }
        return pilotRepository.findAllActivePilotsByCompanyCode(authenticatedCompanyCode());
    }

    public void deactivatePilot(final String pilotEmail, final AssignedFlightsReporter reporter) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR, AisafeRoles.POWER_USER);

        final Email email = Email.valueOf(pilotEmail);
        final Pilot pilot = pilotRepository.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + email));

        final List<AssignedFlightDTO> assignedFlights = buildAssignedFlightDTOs(email);
        reporter.report(assignedFlights);
        if (!assignedFlights.isEmpty()) {
            throw new IllegalStateException(
                    "Pilot cannot be deactivated: " + assignedFlights.size() + " assigned flight(s) found.");
        }

        pilot.deactivate();
        pilotRepository.save(pilot);
    }

    private Iterable<Pilot> allActivePilots() {
        return pilotRepository.findAllActivePilots();
    }

    private List<AssignedFlightDTO> buildAssignedFlightDTOs(final Email email) {
        final List<AssignedFlightDTO> result = new ArrayList<>();
        for (final Flight f : flightRepository.flightsOf(email)) {
            result.add(new AssignedFlightDTO(
                    f.route().value(),
                    f.identity().departureDate(),
                    f.aircraft().value()));
        }
        return result;
    }

    private Email authenticatedUserEmail() {
        final var systemUser = authz.session().orElseThrow().authenticatedUser();
        return userRepository.findBySystemUser(systemUser)
                .orElseThrow(() -> new IllegalStateException("No user profile found"))
                .identity();
    }

    private eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode authenticatedCompanyCode() {
        return collaboratorRepository.findCompanyCollaboratorByEmail(authenticatedUserEmail())
                .orElseThrow(() -> new IllegalStateException("No company collaborator profile found"))
                .companyCode();
    }
}