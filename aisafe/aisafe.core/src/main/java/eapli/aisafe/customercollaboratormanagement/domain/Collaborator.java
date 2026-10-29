package eapli.aisafe.customercollaboratormanagement.domain;

import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "customer_collaborator")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "collaborator_type")
public abstract class Collaborator implements AggregateRoot<Email> {

	private static final long serialVersionUID = 1L;

	@Version
	private Long version;

	@EmbeddedId
	@AttributeOverride(name = "email", column = @Column(name = "email", nullable = false))
	private Email email;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "position", nullable = false))
	private Position position;

	@Embedded
	@AttributeOverride(name = "email", column = @Column(name = "contact_email", nullable = false))
	private Email contactEmail;

	@Embedded
	@AttributeOverride(name = "number", column = @Column(name = "contact_phone_number", nullable = false))
	private PhoneNumber contactPhoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CollaboratorStatus status;

	protected Collaborator(final Email email, final Position position, final Email contactEmail,
			final PhoneNumber contactPhoneNumber) {
		Preconditions.noneNull(email, position, contactEmail, contactPhoneNumber);
		this.email = email;
		this.position = position;
		this.contactEmail = contactEmail;
		this.contactPhoneNumber = contactPhoneNumber;
		this.status = CollaboratorStatus.ACTIVE;
	}

	protected Collaborator() {
		// for ORM
	}

	@Override
	public Email identity() {
		return this.email;
	}

	public Email email() {
		return identity();
	}

	public Position position() {
		return this.position;
	}

	public Email contactEmail() {
		return this.contactEmail;
	}

	public PhoneNumber contactPhoneNumber() {
		return this.contactPhoneNumber;
	}

	public void changeContactDetails(final Email contactEmail, final PhoneNumber contactPhoneNumber) {
		Preconditions.noneNull(contactEmail, contactPhoneNumber);
		this.contactEmail = contactEmail;
		this.contactPhoneNumber = contactPhoneNumber;
	}

	public CollaboratorStatus status() {
		return this.status;
	}

	public boolean isActive() {
		return this.status == CollaboratorStatus.ACTIVE;
	}

	public void disable() {
		if (this.status == CollaboratorStatus.DISABLED) {
			throw new IllegalStateException("Collaborator is already disabled");
		}
		this.status = CollaboratorStatus.DISABLED;
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
