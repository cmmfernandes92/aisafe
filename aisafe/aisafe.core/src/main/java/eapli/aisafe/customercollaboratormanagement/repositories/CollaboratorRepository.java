package eapli.aisafe.customercollaboratormanagement.repositories;

import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.repositories.DomainRepository;

public interface CollaboratorRepository extends DomainRepository<Email, Collaborator> {

	Iterable<CompanyCollaborator> findAllActiveByCompanyCode(CompanyIATACode companyCode);

	Iterable<CompanyCollaborator> findAllByCompanyCode(CompanyIATACode companyCode);

	Iterable<AreaCollaborator> findAllActiveByAreaCode(AreaCode areaCode);

	Iterable<AreaCollaborator> findAllByAreaCode(AreaCode areaCode);

	Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(Email email);

	Optional<AreaCollaborator> findAreaCollaboratorByEmail(Email email);
}
