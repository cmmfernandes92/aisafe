package eapli.aisafe.customercollaboratormanagement.domain;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity(name = "CustomerCompanyCollaborator")
@DiscriminatorValue("COMPANY")
public class CompanyCollaborator extends Collaborator {

	private static final long serialVersionUID = 1L;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "company_iata_code", nullable = true, length = 2))
	private CompanyIATACode companyCode;

	public CompanyCollaborator(final Email email, final Position position, final CompanyIATACode companyCode,
			final Email contactEmail, final PhoneNumber contactPhoneNumber) {
		super(email, position, contactEmail, contactPhoneNumber);
		Preconditions.nonNull(companyCode);
		this.companyCode = companyCode;
	}

	protected CompanyCollaborator() {
		// for ORM
	}

	public CompanyIATACode companyCode() {
		return this.companyCode;
	}
}
