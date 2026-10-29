package eapli.aisafe.customercollaboratormanagement.domain;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity(name = "CustomerAreaCollaborator")
@DiscriminatorValue("AREA")
public class AreaCollaborator extends Collaborator {

	private static final long serialVersionUID = 1L;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = true))
	private AreaCode areaCode;

	public AreaCollaborator(final Email email, final Position position, final AreaCode areaCode,
			final Email contactEmail, final PhoneNumber contactPhoneNumber) {
		super(email, position, contactEmail, contactPhoneNumber);
		Preconditions.nonNull(areaCode);
		this.areaCode = areaCode;
	}

	protected AreaCollaborator() {
		// for ORM
	}

	public AreaCode areaCode() {
		return this.areaCode;
	}
}
