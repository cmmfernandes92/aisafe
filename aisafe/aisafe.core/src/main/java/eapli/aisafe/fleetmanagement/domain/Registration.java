package eapli.aisafe.fleetmanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class Registration implements ValueObject, Comparable<Registration> {

    private static final long serialVersionUID = 1L;

    private String value;

    public Registration(final String value) {
        if (StringPredicates.isNullOrEmpty(value) || value.isBlank()) {
            throw new IllegalArgumentException("Registration must not be null or blank");
        }
        this.value = value.strip().toUpperCase();
    }

    protected Registration() {
        // for ORM
    }

    public static Registration valueOf(final String value) {
        return new Registration(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Registration)) {
            return false;
        }
        return this.value.equals(((Registration) o).value);
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
    public int compareTo(final Registration other) {
        return this.value.compareTo(other.value);
    }
}
