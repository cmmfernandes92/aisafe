package eapli.aisafe.fleetmanagement.application;

import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class ListCompanyFleetController {


    public record FleetEntry(Aircraft aircraft, AircraftModel model) {}

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    public ListCompanyFleetController(final AircraftRepository aircraftRepository,
            final AircraftModelRepository aircraftModelRepository,
            final CollaboratorRepository collaboratorRepository,
            final UserRepository userRepository) {
        Preconditions.noneNull(aircraftRepository, aircraftModelRepository,
                collaboratorRepository, userRepository);
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.userRepository = userRepository;
    }

    public List<FleetEntry> listFleet(final String modelNameFilter,
            final String makerFilter, final Integer minCapacity) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final CompanyIATACode companyCode = resolveCompanyCode();
        final List<FleetEntry> results = new ArrayList<>();
        for (final Aircraft aircraft : aircraftRepository.findAllByCompany(companyCode)) {
            final AircraftModel model = aircraftModelRepository.ofIdentity(aircraft.modelId())
                    .orElse(null);
            if (model == null) {
                continue;
            }
            if (matchesFilters(model, modelNameFilter, makerFilter, minCapacity)) {
                results.add(new FleetEntry(aircraft, model));
            }
        }
        return results;
    }

    private boolean matchesFilters(final AircraftModel model,
            final String modelNameFilter, final String makerFilter, final Integer minCapacity) {
        if (modelNameFilter != null && !modelNameFilter.isBlank()
                && !model.modelName().toString().toLowerCase()
                        .contains(modelNameFilter.toLowerCase())) {
            return false;
        }
        if (makerFilter != null && !makerFilter.isBlank()
                && !model.maker().toString().toLowerCase()
                        .contains(makerFilter.toLowerCase())) {
            return false;
        }
        if (minCapacity != null && minCapacity > 0
                && model.maxPassengerCapacity() < minCapacity) {
            return false;
        }
        return true;
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
