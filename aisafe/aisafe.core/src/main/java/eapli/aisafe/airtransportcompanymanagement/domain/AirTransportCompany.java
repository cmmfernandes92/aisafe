package eapli.aisafe.airtransportcompanymanagement.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;

@Entity
public class AirTransportCompany implements AggregateRoot<CompanyIATACode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private CompanyIATACode iataCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "icao_code", nullable = false, unique = true, length = 3))
    private CompanyICAOCode icaoCode;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    public AirTransportCompany(final CompanyIATACode iataCode, final CompanyICAOCode icaoCode,
            final String companyName) {
        Preconditions.noneNull(iataCode, icaoCode);
        if (StringPredicates.isNullOrEmpty(companyName) || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank");
        }
        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.companyName = companyName.strip();
    }

    protected AirTransportCompany() {
        // for ORM
    }

    public CompanyIATACode iataCode() {
        return this.iataCode;
    }

    public CompanyICAOCode icaoCode() {
        return this.icaoCode;
    }

    public String companyName() {
        return this.companyName;
    }

    @Override
    public CompanyIATACode identity() {
        return this.iataCode;
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
        return companyName + " [" + iataCode + "/" + icaoCode + "]";
    }
}
