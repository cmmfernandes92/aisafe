package eapli.aisafe.fleetmanagement.application;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

import java.util.stream.StreamSupport;

@UseCaseController
public class DecommissionAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public DecommissionAircraftController(final AircraftRepository aircraftRepository,
            final CollaboratorRepository collaboratorRepository,
            final UserRepository userRepository) {
        Preconditions.noneNull(aircraftRepository, collaboratorRepository, userRepository);
        this.aircraftRepository = aircraftRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    public Iterable<Aircraft> listCompanyAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final CompanyIATACode companyCode = resolveCompanyCode();
        return StreamSupport.stream(aircraftRepository.findAllByCompany(companyCode).spliterator(), false)
                .filter(a -> a.status() == OperationalStatus.ACTIVE)
                .toList();
    }

    public Aircraft decommissionAircraft(final String registrationValue) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final CompanyIATACode companyCode = resolveCompanyCode();
        final Registration reg = Registration.valueOf(registrationValue);
        final Aircraft aircraft = aircraftRepository.ofIdentity(reg)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + registrationValue));
        if (!aircraft.companyCode().equals(companyCode)) {
            throw new IllegalArgumentException(
                    "Aircraft " + registrationValue + " does not belong to your company");
        }
        aircraft.decommission();
        return aircraftRepository.save(aircraft);
    }

    private CompanyIATACode resolveCompanyCode() {
        final var systemUser = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session"))
                .authenticatedUser();
        final Email email = userRepository.findBySystemUser(systemUser)
                .orElseThrow(() -> new IllegalStateException("No user profile found for the logged-in user"))
                .identity();
        return collaboratorRepository.findCompanyCollaboratorByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No company collaborator profile found"))
                .companyCode();
    }
}
