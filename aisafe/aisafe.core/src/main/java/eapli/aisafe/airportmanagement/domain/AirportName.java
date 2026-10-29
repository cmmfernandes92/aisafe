package eapli.aisafe.airportmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirportName implements ValueObject, Comparable<AirportName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "airport_name", nullable = false)
    private String value;

    public AirportName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Airport name must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Airport name must not be blank");
        }
        this.value = value.strip();
    }

    protected AirportName() {
        // for ORM
    }

    public static AirportName valueOf(final String value) {
        return new AirportName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirportName)) {
            return false;
        }
        final AirportName that = (AirportName) o;
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
    public int compareTo(final AirportName other) {
        return this.value.compareTo(other.value);
    }
}
