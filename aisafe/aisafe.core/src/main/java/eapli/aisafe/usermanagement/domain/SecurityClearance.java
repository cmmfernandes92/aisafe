package eapli.aisafe.usermanagement.domain;

import java.time.LocalDate;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class SecurityClearance implements ValueObject {

	private static final long serialVersionUID = 1L;

	private LocalDate expiryDate;

	public SecurityClearance(final LocalDate expiryDate) {
		if (expiryDate == null) {
			throw new IllegalArgumentException("Security clearance expiry date is mandatory");
		}
		if (!expiryDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Security clearance expiry date must be in the future");
		}
		this.expiryDate = expiryDate;
	}

	protected SecurityClearance() {
		// for ORM
	}

	public static SecurityClearance valueOf(final LocalDate expiryDate) {
		return new SecurityClearance(expiryDate);
	}

	public LocalDate expiryDate() {
		return this.expiryDate;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SecurityClearance)) {
			return false;
		}
		final SecurityClearance that = (SecurityClearance) o;
		return this.expiryDate.equals(that.expiryDate);
	}

	@Override
	public int hashCode() {
		return this.expiryDate.hashCode();
	}
}
