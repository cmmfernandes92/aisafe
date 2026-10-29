package eapli.aisafe.enginemodelmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class EngineModelName implements ValueObject, Comparable<EngineModelName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "engine_model_name", nullable = false)
    private String value;

    public EngineModelName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Engine model name must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Engine model name must not be blank");
        }
        this.value = value.strip();
    }

    protected EngineModelName() {
        // for ORM
    }

    public static EngineModelName valueOf(final String value) {
        return new EngineModelName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EngineModelName)) return false;
        final EngineModelName that = (EngineModelName) o;
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
    public int compareTo(final EngineModelName other) {
        return this.value.compareTo(other.value);
    }
}