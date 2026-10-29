package eapli.aisafe.airtransportcompanymanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class CompanyICAOCode implements ValueObject, Comparable<CompanyICAOCode> {

    private static final long serialVersionUID = 1L;

    @Column(name = "icao_code", nullable = false, length = 3)
    private String value;

    public CompanyICAOCode(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Company ICAO code must not be null or empty");
        }
        final String trimmed = value.strip().toUpperCase();
        if (!trimmed.matches("[A-Z]{2,3}")) {
            throw new IllegalArgumentException("Company ICAO code must be 2 to 3 letters");
        }
        this.value = trimmed;
    }

    protected CompanyICAOCode() {
        // for ORM
    }

    public static CompanyICAOCode valueOf(final String value) {
        return new CompanyICAOCode(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyICAOCode)) return false;
        final CompanyICAOCode that = (CompanyICAOCode) o;
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
    public int compareTo(final CompanyICAOCode other) {
        return this.value.compareTo(other.value);
    }
}