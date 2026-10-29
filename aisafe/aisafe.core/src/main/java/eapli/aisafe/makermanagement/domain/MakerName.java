package eapli.aisafe.makermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class MakerName implements ValueObject, Comparable<MakerName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "maker_name", nullable = false, unique = true)
    private String value;

    public MakerName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Maker name must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Maker name must not be blank");
        }
        this.value = value.strip();
    }

    protected MakerName() {
        // for ORM
    }

    public static MakerName valueOf(final String value) {
        return new MakerName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MakerName)) return false;
        final MakerName that = (MakerName) o;
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
    public int compareTo(final MakerName other) {
        return this.value.compareTo(other.value);
    }
}