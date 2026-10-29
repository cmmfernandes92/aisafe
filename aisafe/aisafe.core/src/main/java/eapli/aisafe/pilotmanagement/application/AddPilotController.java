package eapli.aisafe.pilotmanagement.application;

import java.util.LinkedHashSet;
import java.util.Set;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.pilotmanagement.repositories.PilotRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class AddPilotController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final PilotRepository pilotRepository;
    private final UserRepository userRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final CollaboratorRepository collaboratorRepository;

    public AddPilotController(final PilotRepository pilotRepository, final UserRepository userRepository,
            final AirTransportCompanyRepository companyRepository, final AircraftModelRepository aircraftModelRepository,
            final CollaboratorRepository collaboratorRepository) {
        Preconditions.noneNull(pilotRepository, userRepository, companyRepository, aircraftModelRepository,
                collaboratorRepository);
        this.pilotRepository = pilotRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.collaboratorRepository = collaboratorRepository;
    }

    public Iterable<AircraftModel> listAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        return aircraftModelRepository.findAll();
    }

    public Pilot addPilot(final String pilotEmail, final Set<Long> aircraftModelIds) {

        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

        Preconditions.nonNull(aircraftModelIds);

        //  1. PRIMEIRO validar contexto autenticado
        final CompanyIATACode companyCode = authenticatedCompanyCode();
        ensureCompanyExists(companyCode);

        //  2. DEPOIS tratar o piloto
        final Email email = Email.valueOf(pilotEmail);

        final User user = userRepository.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist: " + email));

        ensureUserHasPilotRole(user);
        ensureUserIsNotAlreadyPilot(email);

        final Set<AircraftModel> certifications = resolveAircraftModels(aircraftModelIds);

        return pilotRepository.save(new Pilot(email, companyCode, certifications));
    }

    private CompanyIATACode authenticatedCompanyCode() {
        final var systemUser = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session"))
                .authenticatedUser();
        final Email email = userRepository.findBySystemUser(systemUser)
                .orElseThrow(() -> new IllegalStateException("No user profile found"))
                .identity();
        return collaboratorRepository.findCompanyCollaboratorByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No company collaborator profile found"))
                .companyCode();
    }

    private Set<AircraftModel> resolveAircraftModels(final Set<Long> modelIds) {
        if (modelIds.isEmpty()) {
            throw new IllegalArgumentException("A pilot must be certified for at least one aircraft model");
        }
        final Set<AircraftModel> result = new LinkedHashSet<>();
        for (final Long id : modelIds) {
            if (id == null) {
                throw new IllegalArgumentException("Aircraft model id must not be null");
            }
            final AircraftModel model = aircraftModelRepository.ofIdentity(id)
                    .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + id));
            result.add(model);
        }
        return result;
    }

    private void ensureUserHasPilotRole(final User user) {
        if (!user.hasRole(AisafeRoles.PILOT)) {
            throw new IllegalArgumentException("User does not have required role: " + AisafeRoles.PILOT);
        }
    }

    private void ensureUserIsNotAlreadyPilot(final Email email) {
        if (pilotRepository.containsOfIdentity(email)) {
            throw new IllegalArgumentException("User is already linked to a pilot: " + email);
        }
    }

    private void ensureCompanyExists(final CompanyIATACode companyCode) {
        if (companyRepository.findByIataCode(companyCode).isEmpty()) {
            throw new IllegalStateException("Company not found: " + companyCode);
        }
    }
}
