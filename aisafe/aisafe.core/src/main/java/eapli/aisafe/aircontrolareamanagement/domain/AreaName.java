package eapli.aisafe.aircontrolareamanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AreaName implements ValueObject, Comparable<AreaName> {

	private static final long serialVersionUID = 1L;

	private String value;

	public AreaName(final String value) {
		if (StringPredicates.isNullOrEmpty(value) || value.isBlank()) {
			throw new IllegalArgumentException("Area name should neither be null nor blank");
		}
		this.value = value;
	}

	protected AreaName() {
		// for ORM
	}

	public static AreaName valueOf(final String value) {
		return new AreaName(value);
	}

	public String value() {
		return this.value;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AreaName)) {
			return false;
		}
		final AreaName that = (AreaName) o;
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
	public int compareTo(final AreaName other) {
		return this.value.compareTo(other.value);
	}
}
