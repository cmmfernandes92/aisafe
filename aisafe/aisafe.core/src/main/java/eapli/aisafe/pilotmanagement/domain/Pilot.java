package eapli.aisafe.pilotmanagement.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "pilot")
public class Pilot implements AggregateRoot<Email> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "email", column = @Column(name = "pilot_email", nullable = false, unique = true))
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "company_iata_code", nullable = false, length = 2))
    private CompanyIATACode companyCode;

    @ManyToMany
    @JoinTable(name = "pilot_certified_aircraft_models", joinColumns = @JoinColumn(name = "pilot_email", referencedColumnName = "pilot_email"), inverseJoinColumns = @JoinColumn(name = "aircraft_model_id", referencedColumnName = "id"))
    private Set<AircraftModel> certifications = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PilotStatus status;

    public Pilot(final Email email, final CompanyIATACode companyCode, final Set<AircraftModel> certifications) {
        Preconditions.noneNull(email, companyCode, certifications);
        if (certifications.isEmpty()) {
            throw new IllegalArgumentException("A pilot must be certified for at least one aircraft model");
        }
        this.email = email;
        this.companyCode = companyCode;
        this.certifications.addAll(certifications);
        this.status = PilotStatus.ACTIVE;
    }

    protected Pilot() {
        // for ORM
    }

    @Override
    public Email identity() {
        return this.email;
    }

    public CompanyIATACode companyCode() {
        return this.companyCode;
    }

    public Set<AircraftModel> certifications() {
        return Collections.unmodifiableSet(this.certifications);
    }

    public PilotStatus status() {
        return this.status;
    }

    public void deactivate() {
        if (this.status == PilotStatus.INACTIVE) {
            throw new IllegalStateException("Pilot is already inactive.");
        }
        this.status = PilotStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == PilotStatus.ACTIVE;
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
}
