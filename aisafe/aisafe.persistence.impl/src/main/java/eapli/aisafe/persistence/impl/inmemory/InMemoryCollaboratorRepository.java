package eapli.aisafe.persistence.impl.inmemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryCollaboratorRepository extends InMemoryDomainRepository<Collaborator, Email>
		implements CollaboratorRepository {

	@Override
	public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
		return companyCollaboratorsMatching(e -> e.companyCode().equals(companyCode) && e.isActive());
	}

	@Override
	public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
		return companyCollaboratorsMatching(e -> e.companyCode().equals(companyCode));
	}

	@Override
	public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
		return areaCollaboratorsMatching(e -> e.areaCode().equals(areaCode) && e.isActive());
	}

	@Override
	public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
		return areaCollaboratorsMatching(e -> e.areaCode().equals(areaCode));
	}

	@Override
	public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
		return matchOne(e -> e instanceof CompanyCollaborator && e.identity().equals(email))
				.map(e -> (CompanyCollaborator) e);
	}

	@Override
	public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
		return matchOne(e -> e instanceof AreaCollaborator && e.identity().equals(email))
				.map(e -> (AreaCollaborator) e);
	}

	private Iterable<CompanyCollaborator> companyCollaboratorsMatching(
			final java.util.function.Predicate<CompanyCollaborator> predicate) {
		final List<CompanyCollaborator> result = new ArrayList<>();
		for (final Collaborator collaborator : match(e -> e instanceof CompanyCollaborator)) {
			final CompanyCollaborator companyCollaborator = (CompanyCollaborator) collaborator;
			if (predicate.test(companyCollaborator)) {
				result.add(companyCollaborator);
			}
		}
		return result;
	}

	private Iterable<AreaCollaborator> areaCollaboratorsMatching(
			final java.util.function.Predicate<AreaCollaborator> predicate) {
		final List<AreaCollaborator> result = new ArrayList<>();
		for (final Collaborator collaborator : match(e -> e instanceof AreaCollaborator)) {
			final AreaCollaborator areaCollaborator = (AreaCollaborator) collaborator;
			if (predicate.test(areaCollaborator)) {
				result.add(areaCollaborator);
			}
		}
		return result;
	}
}
