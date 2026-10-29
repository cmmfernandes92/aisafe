package eapli.aisafe.fleetmanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class Country implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String name;

    public Country(final String name) {
        if (StringPredicates.isNullOrEmpty(name) || name.isBlank()) {
            throw new IllegalArgumentException("Country name must not be null or blank");
        }
        this.name = name.strip();
    }

    protected Country() {
        // for ORM
    }

    public static Country valueOf(final String name) {
        return new Country(name);
    }

    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Country)) {
            return false;
        }
        return this.name.equals(((Country) o).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
