package eapli.aisafe.countrymanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class CountryName implements ValueObject, Comparable<CountryName> {

    private static final long serialVersionUID = 1L;

    @Column(name = "country_name", nullable = false, unique = true)
    private String value;

    public CountryName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Country name must not be null or empty");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Country name must not be blank");
        }
        this.value = value.strip();
    }

    protected CountryName() {
        // for ORM
    }

    public static CountryName valueOf(final String value) {
        return new CountryName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CountryName)) {
            return false;
        }
        final CountryName that = (CountryName) o;
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
    public int compareTo(final CountryName other) {
        return this.value.compareTo(other.value);
    }
}
