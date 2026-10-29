package eapli.aisafe.customercollaboratormanagement.application;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class RegisterCollaboratorController {

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final CollaboratorRepository collaborators;
	private final UserRepository users;
	private final AirTransportCompanyRepository companies;
	private final AirControlAreaRepository areas;

	public RegisterCollaboratorController(final CollaboratorRepository collaborators, final UserRepository users,
			final AirTransportCompanyRepository companies, final AirControlAreaRepository areas) {
		Preconditions.noneNull(collaborators, users, companies, areas);
		this.collaborators = collaborators;
		this.users = users;
		this.companies = companies;
		this.areas = areas;
	}

	public CompanyCollaborator registerCompanyCollaborator(final String email, final String position,
			final String companyCode) {
		return registerCompanyCollaborator(Email.valueOf(email), Position.valueOf(position),
				CompanyIATACode.valueOf(companyCode));
	}

	public CompanyCollaborator registerCompanyCollaborator(final Email email, final Position position,
			final CompanyIATACode companyCode) {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);

		Preconditions.noneNull(email, position, companyCode);
		final User user = existingUser(email);
		ensureUserHasRole(user, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		ensureUserIsNotAlreadyCollaborator(email);
		ensureCompanyExists(companyCode);

		return collaborators.save(new CompanyCollaborator(email, position, companyCode, email, user.phoneNumber()));
	}

	public AreaCollaborator registerAreaCollaborator(final String email, final String position,
			final String areaCode) {
		return registerAreaCollaborator(Email.valueOf(email), Position.valueOf(position), AreaCode.valueOf(areaCode));
	}

	public AreaCollaborator registerAreaCollaborator(final Email email, final Position position,
			final AreaCode areaCode) {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);

		Preconditions.noneNull(email, position, areaCode);
		final User user = existingUser(email);
		ensureUserHasRole(user, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		ensureUserIsNotAlreadyCollaborator(email);
		ensureAreaExists(areaCode);

		return collaborators.save(new AreaCollaborator(email, position, areaCode, email, user.phoneNumber()));
	}

	private User existingUser(final Email email) {
		return users.ofIdentity(email)
				.orElseThrow(() -> new IllegalArgumentException("User does not exist: " + email));
	}

	private void ensureUserHasRole(final User user, final Role role) {
		if (!user.hasRole(role)) {
			throw new IllegalArgumentException("User does not have required role: " + role);
		}
	}

	private void ensureUserIsNotAlreadyCollaborator(final Email email) {
		if (collaborators.ofIdentity(email).isPresent()) {
			throw new IllegalArgumentException("User is already linked to a collaborator: " + email);
		}
	}

	private void ensureCompanyExists(final CompanyIATACode companyCode) {
		if (companies.findByIataCode(companyCode).isEmpty()) {
			throw new IllegalArgumentException("Air transport company does not exist: " + companyCode);
		}
	}

	private void ensureAreaExists(final AreaCode areaCode) {
		if (areas.findByCode(areaCode).isEmpty()) {
			throw new IllegalArgumentException("Air control area does not exist: " + areaCode);
		}
	}
}
