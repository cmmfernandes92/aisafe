package eapli.aisafe.weather.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

/**
 * Vertical band of a {@link WeatherZone}, expressed in feet. Weather varies with
 * altitude, so a record applies to a contiguous altitude range (e.g. 0-1000 ft).
 */
@Embeddable
public class AltitudeBand implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "alt_lower_ft", nullable = false)
    private double lowerFeet;

    @Column(name = "alt_upper_ft", nullable = false)
    private double upperFeet;

    public AltitudeBand(final double lowerFeet, final double upperFeet) {
        if (lowerFeet < 0) {
            throw new IllegalArgumentException("Lower altitude must be greater than or equal to 0");
        }
        if (upperFeet <= lowerFeet) {
            throw new IllegalArgumentException("Upper altitude must be strictly greater than lower altitude");
        }
        this.lowerFeet = lowerFeet;
        this.upperFeet = upperFeet;
    }

    protected AltitudeBand() {
        // for ORM
    }

    public double lowerFeet() {
        return this.lowerFeet;
    }

    public double upperFeet() {
        return this.upperFeet;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AltitudeBand)) {
            return false;
        }
        final AltitudeBand that = (AltitudeBand) o;
        return Double.compare(this.lowerFeet, that.lowerFeet) == 0
                && Double.compare(this.upperFeet, that.upperFeet) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(this.lowerFeet) + Double.hashCode(this.upperFeet);
    }

    @Override
    public String toString() {
        return this.lowerFeet + "-" + this.upperFeet + " ft";
    }
}