package eapli.aisafe.weather.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class WeatherDataId implements ValueObject, Comparable<WeatherDataId>, Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "weather_data_id", nullable = false, updatable = false)
    private UUID id;

    public static WeatherDataId newId() {
        final WeatherDataId wid = new WeatherDataId();
        wid.id = UUID.randomUUID();
        return wid;
    }

    /**
     * Reconstructs an id from its string form (the UUID produced by
     * {@link #toString()}). Used to read back a stored weather choice — e.g. the
     * per-segment {@code WeatherAssignment} persisted by US082 — into an id the
     * repository can resolve.
     */
    public static WeatherDataId valueOf(final String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Weather data id must not be empty");
        }
        final WeatherDataId wid = new WeatherDataId();
        wid.id = UUID.fromString(id);
        return wid;
    }

    protected WeatherDataId() {
        // for ORM
    }

    public UUID id() {
        return this.id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherDataId)) {
            return false;
        }
        return this.id.equals(((WeatherDataId) o).id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(final WeatherDataId other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
