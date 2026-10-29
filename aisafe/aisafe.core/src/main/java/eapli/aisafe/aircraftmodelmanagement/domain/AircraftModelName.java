package eapli.aisafe.aircraftmodelmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AircraftModelName implements ValueObject, Comparable<AircraftModelName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "model_name", nullable = false)
    private String value;

    public AircraftModelName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Aircraft model name must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Aircraft model name must not be blank");
        }
        this.value = value.strip();
    }

    protected AircraftModelName() {
        // for ORM
    }

    public static AircraftModelName valueOf(final String value) {
        return new AircraftModelName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AircraftModelName)) return false;
        final AircraftModelName that = (AircraftModelName) o;
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
    public int compareTo(final AircraftModelName other) {
        return this.value.compareTo(other.value);
    }
}
