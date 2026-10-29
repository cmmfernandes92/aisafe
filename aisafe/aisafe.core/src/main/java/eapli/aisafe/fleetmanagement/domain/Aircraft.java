package eapli.aisafe.fleetmanagement.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;

import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class Aircraft implements AggregateRoot<Registration> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "registration", length = 10))
    private Registration registration;

    @Column(name = "aircraft_model_id", nullable = false)
    private Long modelId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "company_iata_code", length = 2, nullable = false))
    private CompanyIATACode companyCode;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "registration_country", nullable = false))
    private Country registrationCountry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationalStatus status;

    public Aircraft(final Registration registration, final Long modelId, final CompanyIATACode companyCode,
            final CabinConfiguration cabinConfiguration, final Country registrationCountry,
            final int maxPassengerCapacity) {
        Preconditions.noneNull(registration, modelId, companyCode, cabinConfiguration, registrationCountry);
        if (maxPassengerCapacity > 0 && cabinConfiguration.totalSeats() > maxPassengerCapacity) {
            throw new IllegalArgumentException(
                    "Total seat count (" + cabinConfiguration.totalSeats()
                            + ") exceeds model capacity (" + maxPassengerCapacity + ")");
        }
        this.registration = registration;
        this.modelId = modelId;
        this.companyCode = companyCode;
        this.cabinConfiguration = cabinConfiguration;
        this.registrationCountry = registrationCountry;
        this.status = OperationalStatus.ACTIVE;
    }

    protected Aircraft() {
        // for ORM
    }

    @Override
    public Registration identity() {
        return this.registration;
    }

    public Registration registration() {
        return this.registration;
    }

    public Long modelId() {
        return this.modelId;
    }

    public CompanyIATACode companyCode() {
        return this.companyCode;
    }

    public CabinConfiguration cabinConfiguration() {
        return this.cabinConfiguration;
    }

    public Country registrationCountry() {
        return this.registrationCountry;
    }

    public OperationalStatus status() {
        return this.status;
    }

    public void decommission() {
        if (this.status == OperationalStatus.RETIRED) {
            throw new IllegalStateException("Aircraft is already retired");
        }
        this.status = OperationalStatus.RETIRED;
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
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public String toString() {
        return registration + " [" + status + "]";
    }
}
