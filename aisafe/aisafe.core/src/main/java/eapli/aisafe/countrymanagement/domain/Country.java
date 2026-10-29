package eapli.aisafe.countrymanagement.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class Country implements AggregateRoot<CountryCode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "country_code"))
    private CountryCode code;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "country_name", nullable = false, unique = true))
    private CountryName name;

    public Country(final CountryCode code, final CountryName name) {
        Preconditions.noneNull(code, name);
        this.code = code;
        this.name = name;
    }

    protected Country() {
        // for ORM
    }

    public static Country valueOf(final String code, final String name) {
        return new Country(CountryCode.valueOf(code), CountryName.valueOf(name));
    }

    @Override
    public CountryCode identity() {
        return this.code;
    }

    public CountryCode code() {
        return identity();
    }

    public CountryName name() {
        return this.name;
    }

    public void changeName(final CountryName name) {
        Preconditions.nonNull(name, "Country name must not be null");
        this.name = name;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return this.name + " (" + this.code + ")";
    }
}
