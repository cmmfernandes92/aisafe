package eapli.aisafe.countrymanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class CountryCode implements ValueObject, Comparable<CountryCode> {

    private static final long serialVersionUID = 1L;

    @Column(name = "country_code", nullable = false, unique = true)
    private String value;

    public CountryCode(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Country code must not be null or empty");
        }
        final String normalized = value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Country code must not be blank");
        }
        if (!normalized.matches("\\d{1,3}")) {
            throw new IllegalArgumentException("Country code must contain one to three digits");
        }
        this.value = normalized;
    }

    protected CountryCode() {
        // for ORM
    }

    public static CountryCode valueOf(final String value) {
        return new CountryCode(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CountryCode)) {
            return false;
        }
        final CountryCode that = (CountryCode) o;
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
    public int compareTo(final CountryCode other) {
        return this.value.compareTo(other.value);
    }
}
