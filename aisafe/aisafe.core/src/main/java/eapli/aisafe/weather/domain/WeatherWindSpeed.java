package eapli.aisafe.weather.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class WeatherWindSpeed implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "wind_speed", nullable = false)
    private double value;

    public WeatherWindSpeed(final double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Wind speed must be greater than or equal to 0");
        }
        this.value = value;
    }

    protected WeatherWindSpeed() {
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
        if (!(o instanceof WeatherWindSpeed)) {
            return false;
        }
        return Double.compare(this.value, ((WeatherWindSpeed) o).value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    @Override
    public String toString() {
        return this.value + " kt";
    }
}
