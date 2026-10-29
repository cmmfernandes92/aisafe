package eapli.aisafe.weather.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class WeatherWindDirection implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "wind_direction", nullable = false)
    private double value;

    public WeatherWindDirection(final double value) {
        if (value < 0 || value > 360) {
            throw new IllegalArgumentException("Wind direction must be between 0 and 360 degrees");
        }
        this.value = value;
    }

    protected WeatherWindDirection() {
        // for ORM
    }

    public double value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherWindDirection)) {
            return false;
        }
        return Double.compare(this.value, ((WeatherWindDirection) o).value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    @Override
    public String toString() {
        return this.value + "°";
    }
}
