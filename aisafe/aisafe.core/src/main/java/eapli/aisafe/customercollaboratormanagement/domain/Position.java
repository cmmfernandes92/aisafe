package eapli.aisafe.customercollaboratormanagement.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Position implements ValueObject, Comparable<Position> {

	private static final long serialVersionUID = 1L;

	@Column(name = "position", nullable = false)
	private String value;

	public Position(final String value) {
		if (StringPredicates.isNullOrEmpty(value) || value.isBlank()) {
			throw new IllegalArgumentException("Position must not be blank");
		}
		this.value = value.strip();
	}

	protected Position() {
		// for ORM
	}

	public static Position valueOf(final String value) {
		return new Position(value);
	}

	public String value() {
		return this.value;
	}

	@Override
	public int compareTo(final Position other) {
		return this.value.compareTo(other.value);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Position)) {
			return false;
		}
		final Position that = (Position) o;
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
}
