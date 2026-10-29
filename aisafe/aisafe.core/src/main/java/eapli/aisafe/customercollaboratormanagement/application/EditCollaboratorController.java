package eapli.aisafe.customercollaboratormanagement.application;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

/**
 * Application service for US063 — Edit a customer's collaborator.
 *
 * <p>Updates contactEmail and contactPhoneNumber on the Collaborator aggregate.
 * These are business contact details — distinct from the login/identity email
 * which is the @EmbeddedId and is immutable.
 *
 * <p>Authorization: BACKOFFICE_OPERATOR only.
 */
@UseCaseController
public class EditCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository collaborators;

    public EditCollaboratorController(final CollaboratorRepository collaborators) {
        Preconditions.nonNull(collaborators);
        this.collaborators = collaborators;
    }

    /**
     * Returns all ACTIVE company collaborators for a given company (for UI selection list).
     *
     * @param companyCode the IATA code of the company to filter by
     * @return active collaborators of that company
     */
    public Iterable<CompanyCollaborator> activeCollaboratorsOf(final CompanyIATACode companyCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);
        Preconditions.nonNull(companyCode);
        return collaborators.findAllActiveByCompanyCode(companyCode);
    }

    /**
     * Updates the business contact email and phone number of a collaborator (US063).
     *
     * <p>The login/identity email is immutable and is NOT affected.
     * Delegates to {@code Collaborator.changeContactDetails()} which enforces
     * that both values are non-null.
     *
     * @param collaboratorEmail the login email identifying the collaborator
     * @param newContactEmail   the new business contact email
     * @param newPhoneNumber    the new business contact phone number
     * @return the updated collaborator
     * @throws IllegalArgumentException if collaborator not found, or email/phone invalid
     */
    public Collaborator editContactInfo(final String collaboratorEmail,
                                        final String newContactEmail, final String newPhoneNumber) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);

        final Email email = Email.valueOf(collaboratorEmail);

        final Collaborator collaborator = collaborators.ofIdentity(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No collaborator found with email: " + email));

        // Domain enforces non-null on both arguments
        collaborator.changeContactDetails(
                Email.valueOf(newContactEmail),
                new PhoneNumber(newPhoneNumber));

        return collaborators.save(collaborator);
    }
}