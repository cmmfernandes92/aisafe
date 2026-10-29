package eapli.aisafe.flightroutemanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class RouteName implements ValueObject, Comparable<RouteName> {

    private static final long serialVersionUID = 1L;

    private static final String PATTERN = "[A-Z]{2}[0-9]{1,4}";

    @Column(name = "route_name", nullable = false, unique = true, length = 6)
    private String value;

    public RouteName(final String value) {
        if (StringPredicates.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Route name must not be null or empty");
        }
        final String normalised = value.strip().toUpperCase();
        if (!normalised.matches(PATTERN)) {
            throw new IllegalArgumentException(
                    "Route name must be 2 letters followed by 1 to 4 digits (e.g. TP123)");
        }
        this.value = normalised;
    }

    protected RouteName() {
        // for ORM
    }

    public static RouteName valueOf(final String value) {
        return new RouteName(value);
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteName)) return false;
        final RouteName that = (RouteName) o;
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
    public int compareTo(final RouteName other) {
        return this.value.compareTo(other.value);
    }
}