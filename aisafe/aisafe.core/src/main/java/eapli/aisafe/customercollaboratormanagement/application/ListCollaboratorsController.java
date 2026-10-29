package eapli.aisafe.customercollaboratormanagement.application;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

@UseCaseController
public class ListCollaboratorsController {

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final CollaboratorRepository collaborators;
	private final UserRepository users;
	private final AirTransportCompanyRepository companies;
	private final AirControlAreaRepository areas;

	public ListCollaboratorsController(final CollaboratorRepository collaborators, final UserRepository users,
			final AirTransportCompanyRepository companies, final AirControlAreaRepository areas) {
		Preconditions.noneNull(collaborators, users, companies, areas);
		this.collaborators = collaborators;
		this.users = users;
		this.companies = companies;
		this.areas = areas;
	}

	public Iterable<AirTransportCompany> allCompanies() {
		ensureBackofficeOperator();
		return companies.findAll();
	}

	public Iterable<AirControlArea> allAreas() {
		ensureBackofficeOperator();
		return areas.findAll();
	}

	public Iterable<CompanyCollaborator> activeCollaboratorsOf(final CompanyIATACode companyCode) {
		ensureBackofficeOperator();
		Preconditions.nonNull(companyCode);
		ensureCompanyExists(companyCode);
		return collaborators.findAllActiveByCompanyCode(companyCode);
	}

	public Iterable<AreaCollaborator> activeCollaboratorsOf(final AreaCode areaCode) {
		ensureBackofficeOperator();
		Preconditions.nonNull(areaCode);
		ensureAreaExists(areaCode);
		return collaborators.findAllActiveByAreaCode(areaCode);
	}

	public Optional<User> userOf(final Email email) {
		ensureBackofficeOperator();
		Preconditions.nonNull(email);
		return users.ofIdentity(email);
	}

	private void ensureBackofficeOperator() {
		authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);
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
