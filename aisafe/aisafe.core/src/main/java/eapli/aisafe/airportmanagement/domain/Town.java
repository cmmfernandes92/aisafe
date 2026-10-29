package eapli.aisafe.airportmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class Town implements ValueObject, Comparable<Town> {

    private static final long serialVersionUID = 1L;

    @Column(name = "town", nullable = false)
    private String value;

    public Town(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Town must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Town must not be blank");
        }
        this.value = value.strip();
    }

    protected Town() {
        // for ORM
    }

    public static Town valueOf(final String value) {
        return new Town(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Town)) {
            return false;
        }
        final Town that = (Town) o;
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
    public int compareTo(final Town other) {
        return this.value.compareTo(other.value);
    }
}
