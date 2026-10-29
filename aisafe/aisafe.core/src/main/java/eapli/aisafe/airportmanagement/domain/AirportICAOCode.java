package eapli.aisafe.airportmanagement.domain;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class AirportICAOCode implements ValueObject, Comparable<AirportICAOCode> {

    private static final long serialVersionUID = 1L;

    @Column(name = "icao_code", nullable = false, unique = true)
    private String value;

    public AirportICAOCode(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Airport ICAO code must not be null or empty");
        }
        final String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{4}")) {
            throw new IllegalArgumentException("Airport ICAO code must contain exactly four letters");
        }
        this.value = normalized;
    }

    protected AirportICAOCode() {
        // for ORM
    }

    public static AirportICAOCode valueOf(final String value) {
        return new AirportICAOCode(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AirportICAOCode)) {
            return false;
        }
        final AirportICAOCode that = (AirportICAOCode) o;
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
    public int compareTo(final AirportICAOCode other) {
        return this.value.compareTo(other.value);
    }
}
