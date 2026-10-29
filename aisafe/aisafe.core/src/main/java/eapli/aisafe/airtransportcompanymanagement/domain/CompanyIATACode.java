package eapli.aisafe.airtransportcompanymanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class CompanyIATACode implements ValueObject, Comparable<CompanyIATACode> {

    private static final long serialVersionUID = 1L;

    @Column(name = "iata_code", nullable = false, length = 2)
    private String value;

    public CompanyIATACode(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Company IATA code must not be null or empty");
        }
        final String trimmed = value.strip().toUpperCase();
        if (!trimmed.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Company IATA code must be exactly 2 letters");
        }
        this.value = trimmed;
    }

    protected CompanyIATACode() {
        // for ORM
    }

    public static CompanyIATACode valueOf(final String value) {
        return new CompanyIATACode(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyIATACode)) return false;
        final CompanyIATACode that = (CompanyIATACode) o;
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
    public int compareTo(final CompanyIATACode other) {
        return this.value.compareTo(other.value);
    }
}