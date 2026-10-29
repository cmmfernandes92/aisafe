package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CollaboratorStatus;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaCollaboratorRepository extends JpaAutoTxRepository<Collaborator, Email, Email>
		implements CollaboratorRepository {

	public JpaCollaboratorRepository(final String puname) {
		super(puname, Application.settings().getExtendedPersistenceProperties(), "email");
	}

	@Override
	public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
		final Map<String, Object> params = new HashMap<>();
		params.put("companyCode", companyCode);
		params.put("status", CollaboratorStatus.ACTIVE);
		return match("TYPE(e) = CustomerCompanyCollaborator AND e.companyCode = :companyCode AND e.status = :status", params)
				.stream().map(e -> (CompanyCollaborator) e).toList();
	}

	@Override
	public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
		final Map<String, Object> params = new HashMap<>();
		params.put("companyCode", companyCode);
		return match("TYPE(e) = CustomerCompanyCollaborator AND e.companyCode = :companyCode", params)
				.stream().map(e -> (CompanyCollaborator) e).toList();
	}

	@Override
	public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
		final Map<String, Object> params = new HashMap<>();
		params.put("areaCode", areaCode);
		params.put("status", CollaboratorStatus.ACTIVE);
		return match("TYPE(e) = CustomerAreaCollaborator AND e.areaCode = :areaCode AND e.status = :status", params)
				.stream().map(e -> (AreaCollaborator) e).toList();
	}

	@Override
	public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
		final Map<String, Object> params = new HashMap<>();
		params.put("areaCode", areaCode);
		return match("TYPE(e) = CustomerAreaCollaborator AND e.areaCode = :areaCode", params)
				.stream().map(e -> (AreaCollaborator) e).toList();
	}

	@Override
	public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
		final Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		return matchOne("TYPE(e) = CustomerCompanyCollaborator AND e.email = :email", params)
				.map(e -> (CompanyCollaborator) e);
	}

	@Override
	public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
		final Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		return matchOne("TYPE(e) = CustomerAreaCollaborator AND e.email = :email", params)
				.map(e -> (AreaCollaborator) e);
	}
}
