package eapli.aisafe.airportmanagement.domain;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirportIATACode implements ValueObject, Comparable<AirportIATACode> {

    private static final long serialVersionUID = 1L;

    @Column(name = "iata_code", nullable = false, unique = true)
    private String value;

    public AirportIATACode(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Airport IATA code must not be null or empty");
        }
        final String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Airport IATA code must contain exactly three letters");
        }
        this.value = normalized;
    }

    protected AirportIATACode() {
        // for ORM
    }

    public static AirportIATACode valueOf(final String value) {
        return new AirportIATACode(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirportIATACode)) {
            return false;
        }
        final AirportIATACode that = (AirportIATACode) o;
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
    public int compareTo(final AirportIATACode other) {
        return this.value.compareTo(other.value);
    }
}
