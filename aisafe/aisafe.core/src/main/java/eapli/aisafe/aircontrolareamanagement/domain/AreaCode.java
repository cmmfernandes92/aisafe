package eapli.aisafe.aircontrolareamanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AreaCode implements ValueObject, Comparable<AreaCode> {

	private static final long serialVersionUID = 1L;

	private String value;

	public AreaCode(final String value) {
		if (StringPredicates.isNullOrEmpty(value) || value.isBlank()) {
			throw new IllegalArgumentException("Area code should neither be null nor blank");
		}
		this.value = value;
	}

	protected AreaCode() {
		// for ORM
	}

	public static AreaCode valueOf(final String value) {
		return new AreaCode(value);
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AreaCode)) {
			return false;
		}
		final AreaCode that = (AreaCode) o;
		return this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Override
	public int compareTo(final AreaCode other) {
		return this.value.compareTo(other.value);
	}
}