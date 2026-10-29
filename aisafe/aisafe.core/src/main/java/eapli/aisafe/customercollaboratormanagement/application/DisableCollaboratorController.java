package eapli.aisafe.customercollaboratormanagement.application;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.validations.Preconditions;

/**
 * Application service for US064 — Disable a customer's collaborator.
 *
 * <p>Authorization: BACKOFFICE_OPERATOR only.
 */
@UseCaseController
public class DisableCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final CollaboratorRepository collaborators;
    private final UserRepository users;

    public DisableCollaboratorController(final CollaboratorRepository collaborators,
                                         final UserRepository users) {
        Preconditions.noneNull(collaborators, users);
        this.collaborators = collaborators;
        this.users         = users;
    }

    /**
     * Returns all ACTIVE company collaborators for a given company (for UI selection list).
     *
     * @param companyCode the IATA code of the company
     * @return active collaborators of that company
     */
    public Iterable<CompanyCollaborator> activeCollaboratorsOf(final CompanyIATACode companyCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);
        Preconditions.nonNull(companyCode);
        return collaborators.findAllActiveByCompanyCode(companyCode);
    }

    /**
     * Disables a collaborator so they may no longer use the system (US064).
     *
     * @param collaboratorEmail the email identifying the collaborator
     * @return the disabled collaborator
     * @throws IllegalArgumentException if not found
     * @throws IllegalStateException    if already disabled
     */
    public Collaborator disableCollaborator(final String collaboratorEmail) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);

        final Email email = Email.valueOf(collaboratorEmail);

        final Collaborator collaborator = collaborators.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No collaborator found with email: " + email));

        // Domain enforces: throws IllegalStateException if already DISABLED
        collaborator.disable();

        // Deactivate the SystemUser so the collaborator cannot authenticate (US064.AC2)
        final User user = users.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No user found with email: " + email));
        userSvc.deactivateUser(user.systemUser());

        return collaborators.save(collaborator);
    }
}