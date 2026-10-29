package eapli.aisafe.flightroutemanagement.domain;

import java.time.LocalDate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class FlightRoute implements AggregateRoot<RouteName> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "route_name", nullable = false, unique = true, length = 6))
    private RouteName routeName;

    @ManyToOne(optional = false)
    private AirTransportCompany company;

    @ManyToOne(optional = false)
    private Airport origin;

    @ManyToOne(optional = false)
    private Airport destination;

    @Column(name = "active", nullable = false)
    private boolean active;

    // US074: date from which the route is no longer usable.
    // Null while active; set when the route is deactivated.
    @Column(name = "deactivated_from")
    private LocalDate deactivatedFrom;

    public FlightRoute(final RouteName routeName, final AirTransportCompany company,
            final Airport origin, final Airport destination) {
        Preconditions.noneNull(routeName, company, origin, destination);
        if (origin.sameAs(destination)) {
            throw new IllegalArgumentException("Origin and destination airports must be different");
        }
        this.routeName = routeName;
        this.company = company;
        this.origin = origin;
        this.destination = destination;
        this.active = true;
        this.deactivatedFrom = null;
    }

    protected FlightRoute() {
        // for ORM
    }

    @Override
    public RouteName identity() {
        return this.routeName;
    }

    public RouteName routeName() {
        return this.routeName;
    }

    public AirTransportCompany company() {
        return this.company;
    }

    public Airport origin() {
        return this.origin;
    }

    public Airport destination() {
        return this.destination;
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * US074 - Deactivates this route as of {@code from} (soft delete).
     * Tell-Don't-Ask: enforces 'must be active' invariant; records deactivatedFrom.
     */
    public void deactivate(final LocalDate from) {
        Preconditions.noneNull(from);
        if (!this.active) {
            throw new IllegalStateException("Flight route is already inactive: " + this.routeName);
        }
        this.active = false;
        this.deactivatedFrom = from;
    }

    /** @return the effective deactivation date, or {@code null} if the route is active. */
    public LocalDate deactivatedFrom() {
        return this.deactivatedFrom;
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
        return routeName + " (" + origin + " → " + destination + ")";
    }
}