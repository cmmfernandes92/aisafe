package eapli.aisafe.usermanagement.domain;

import java.util.regex.Pattern;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;
import jakarta.persistence.Embeddable;

@Embeddable
public class Email implements ValueObject, Comparable<Email> {

	private static final long serialVersionUID = 1L;
	private static final Pattern EMAIL_PATTERN = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private String email;

	public Email(final String email) {
		if (StringPredicates.isNullOrEmpty(email)) {
			throw new IllegalArgumentException("Email must not be null or empty");
		}

		final String normalized = email.strip();
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Email has invalid format");
		}

		this.email = normalized;
	}

	protected Email() {
		// for ORM
	}

	public static Email valueOf(final String email) {
		return new Email(email);
	}

	@Override
	public int compareTo(final Email other) {
		return this.email.compareTo(other.email);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Email)) {
			return false;
		}
		final Email that = (Email) o;
		return this.email.equals(that.email);
	}

	@Override
	public int hashCode() {
		return this.email.hashCode();
	}

	@Override
	public String toString() {
		return this.email;
	}
}
