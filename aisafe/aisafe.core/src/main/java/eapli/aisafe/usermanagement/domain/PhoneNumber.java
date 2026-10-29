package eapli.aisafe.usermanagement.domain;

import java.util.regex.Pattern;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;
import jakarta.persistence.Embeddable;

@Embeddable
public class PhoneNumber implements ValueObject {

	private static final long serialVersionUID = 1L;
	private static final Pattern INTERNATIONAL_PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

	private String number;

	public PhoneNumber(final String number) {
		if (StringPredicates.isNullOrEmpty(number)) {
			throw new IllegalArgumentException("Phone number must not be null or empty");
		}
		final String normalized = number.strip();
		if (!INTERNATIONAL_PHONE_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Phone number must follow international format (e.g. +351912345678)");
		}
		this.number = normalized;
	}

	protected PhoneNumber() {
		// for ORM
	}

	public static PhoneNumber valueOf(final String number) {
		return new PhoneNumber(number);
	}

	public String number() {
		return this.number;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PhoneNumber)) {
			return false;
		}
		final PhoneNumber that = (PhoneNumber) o;
		return this.number.equals(that.number);
	}

	@Override
	public int hashCode() {
		return this.number.hashCode();
	}

	@Override
	public String toString() {
		return this.number;
	}
}
