package eapli.aisafe.flightmanagement.domain;

import java.io.Serializable;
import java.time.LocalDate;

import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;

/**
 * Identity of a {@link Flight}: the route plus the departure date.
 * This makes "one flight per route per day" (US080.6 / I4) a structural
 * uniqueness constraint rather than a runtime check.
 */
@Embeddable
public class FlightId implements ValueObject, Comparable<FlightId>, Serializable {

    private static final long serialVersionUID = 1L;

    @Embedded
    private RouteName routeName;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    public FlightId(final RouteName routeName, final LocalDate departureDate) {
        Preconditions.noneNull(routeName, departureDate);
        this.routeName = routeName;
        this.departureDate = departureDate;
    }

    protected FlightId() {
        // for ORM
    }

    public RouteName routeName() {
        return this.routeName;
    }

    public LocalDate departureDate() {
        return this.departureDate;
    }

    @Override
    public int compareTo(final FlightId other) {
        final int byRoute = this.routeName.compareTo(other.routeName);
        return byRoute != 0 ? byRoute : this.departureDate.compareTo(other.departureDate);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlightId)) {
            return false;
        }
        final FlightId that = (FlightId) o;
        return this.routeName.equals(that.routeName)
                && this.departureDate.equals(that.departureDate);
    }

    @Override
    public int hashCode() {
        return 31 * routeName.hashCode() + departureDate.hashCode();
    }

    @Override
    public String toString() {
        return routeName + "@" + departureDate;
    }
}
