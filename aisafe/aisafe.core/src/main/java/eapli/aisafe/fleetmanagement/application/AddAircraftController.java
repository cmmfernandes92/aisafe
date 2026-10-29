package eapli.aisafe.fleetmanagement.application;

import java.util.Map;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class AddAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final AircraftModelRepository aircraftModelRepository;
    private final UserRepository userRepository;

    public AddAircraftController(final AircraftRepository aircraftRepository,
            final CollaboratorRepository collaboratorRepository,
            final AircraftModelRepository aircraftModelRepository,
            final UserRepository userRepository) {
        Preconditions.noneNull(aircraftRepository, collaboratorRepository,
                aircraftModelRepository, userRepository);
        this.aircraftRepository = aircraftRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.userRepository = userRepository;
    }

    public CompanyIATACode authenticatedCompanyCode() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
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

    public Iterable<AircraftModel> listModels() {
        return aircraftModelRepository.findAll();
    }

    public Aircraft addAircraft(final String registration, final Long modelId,
            final Map<SeatClass, Integer> seats, final String country) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        final CompanyIATACode companyCode = authenticatedCompanyCode();
        return doRegister(registration, modelId, seats, country, companyCode);
    }

    public Aircraft addAircraftForCompany(final String registration, final Long modelId,
            final Map<SeatClass, Integer> seats, final String country, final String companyIataCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.POWER_USER, AisafeRoles.BACKOFFICE_OPERATOR);
        return doRegister(registration, modelId, seats, country, CompanyIATACode.valueOf(companyIataCode));
    }

    private Aircraft doRegister(final String registration, final Long modelId,
            final Map<SeatClass, Integer> seats, final String country,
            final CompanyIATACode companyCode) {
        final Registration reg = Registration.valueOf(registration);
        if (aircraftRepository.containsOfIdentity(reg)) {
            throw new IllegalArgumentException(
                    "An aircraft with registration " + registration + " is already registered");
        }
        final AircraftModel model = aircraftModelRepository.ofIdentity(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft model not found: " + modelId));
        final CabinConfiguration cabin = new CabinConfiguration(seats);
        final Country registrationCountry = new Country(country);
        final Aircraft aircraft = new Aircraft(reg, modelId, companyCode, cabin,
                registrationCountry, model.maxPassengerCapacity());
        return aircraftRepository.save(aircraft);
    }
}
