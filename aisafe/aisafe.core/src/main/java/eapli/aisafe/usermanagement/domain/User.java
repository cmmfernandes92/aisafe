package eapli.aisafe.usermanagement.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "aisafe_user")
public class User implements AggregateRoot<Email> {

	private static final long serialVersionUID = 1L;

	@Version
	private Long version;

	@EmbeddedId
	private Email email;

	@OneToOne
	@JoinColumn(name = "system_user_id", nullable = false, unique = true)
	private SystemUser systemUser;

	@Embedded
	@AttributeOverride(name = "number", column = @Column(name = "phone_number", nullable = false))
	private PhoneNumber phoneNumber;

	@Embedded
	@AttributeOverride(name = "expiryDate", column = @Column(name = "security_clearance_expiry_date", nullable = false))
	private SecurityClearance securityClearance;

	@Embedded
	@AttributeOverride(name = "lastAssessmentDate", column = @Column(name = "skills_assessment_date", nullable = false))
	private SkillsAssessment skillsAssessment;

	public User(final SystemUser systemUser, final Email email, final PhoneNumber phoneNumber,
				final SecurityClearance securityClearance, final SkillsAssessment skillsAssessment) {
		Preconditions.noneNull(systemUser, email, phoneNumber, securityClearance, skillsAssessment);
		this.systemUser = systemUser;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.securityClearance = securityClearance;
		this.skillsAssessment = skillsAssessment;
	}

	protected User() {
		// for ORM
	}

	public SystemUser systemUser() {
		return this.systemUser;
	}

	public PhoneNumber phoneNumber() {
		return this.phoneNumber;
	}

	public SecurityClearance securityClearance() {
		return this.securityClearance;
	}

	public SkillsAssessment skillsAssessment() {
		return this.skillsAssessment;
	}

	public boolean hasRole(final Role role) {
		return this.systemUser.hasAny(role);
	}

	public void updatePhoneNumber(final PhoneNumber newPhoneNumber) {
		Preconditions.nonNull(newPhoneNumber, "Phone number must not be null");
		this.phoneNumber = newPhoneNumber;
	}

	@Override
	public Email identity() {
		return this.email;
	}

	@Override
	public boolean sameAs(final Object other) {
		return DomainEntities.areEqual(this, other);
	}

	@Override
	public boolean equals(final Object o) {
		return DomainEntities.areEqual(this, o);
	}

	@Override
	public int hashCode() {
		return DomainEntities.hashCode(this);
	}
}