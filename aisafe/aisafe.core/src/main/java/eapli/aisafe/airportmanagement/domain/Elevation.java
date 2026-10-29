package eapli.aisafe.airportmanagement.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class Elevation implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "elevation_meters", nullable = false)
    private double value;

    public Elevation(final double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Elevation must be a finite number");
        }
        this.value = value;
    }

    protected Elevation() {
        // for ORM
    }

    public static Elevation valueOf(final double value) {
        return new Elevation(value);
    }

    public double value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Elevation)) {
            return false;
        }
        final Elevation that = (Elevation) o;
        return Double.compare(this.value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public String toString() {
        return Double.toString(this.value);
    }
}
