package eapli.aisafe.weather.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.framework.domain.model.ValueObject;

/**
 * Sub-area of an air control area to which a weather record applies: a bounding
 * box (southwest / northeast corners) together with an {@link AltitudeBand}.
 * Weather is not uniform across a whole area, so a record is zone-banded.
 * <p>
 * {@link Coordinates} is reused from the AirControlArea aggregate (no duplication);
 * the instance belongs to the WeatherData aggregate.
 */
@Embeddable
public class WeatherZone implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "zone_sw_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "zone_sw_longitude"))
    })
    private Coordinates southwest;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "zone_ne_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "zone_ne_longitude"))
    })
    private Coordinates northeast;

    @Embedded
    private AltitudeBand altitudeBand;

    public WeatherZone(final Coordinates southwest, final Coordinates northeast,
            final AltitudeBand altitudeBand) {
        if (southwest == null) {
            throw new IllegalArgumentException("Southwest coordinate must not be null");
        }
        if (northeast == null) {
            throw new IllegalArgumentException("Northeast coordinate must not be null");
        }
        if (altitudeBand == null) {
            throw new IllegalArgumentException("Altitude band must not be null");
        }
        if (southwest.latitude() >= northeast.latitude()) {
            throw new IllegalArgumentException(
                    "Southwest latitude must be strictly less than northeast latitude");
        }
        if (southwest.longitude() >= northeast.longitude()) {
            throw new IllegalArgumentException(
                    "Southwest longitude must be strictly less than northeast longitude");
        }
        this.southwest = southwest;
        this.northeast = northeast;
        this.altitudeBand = altitudeBand;
    }

    protected WeatherZone() {
        // for ORM
    }

    public Coordinates southwest() {
        return this.southwest;
    }

    public Coordinates northeast() {
        return this.northeast;
    }

    public AltitudeBand altitudeBand() {
        return this.altitudeBand;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherZone)) {
            return false;
        }
        final WeatherZone that = (WeatherZone) o;
        return this.southwest.equals(that.southwest)
                && this.northeast.equals(that.northeast)
                && this.altitudeBand.equals(that.altitudeBand);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.southwest.hashCode() + this.northeast.hashCode())
                + this.altitudeBand.hashCode();
    }

    @Override
    public String toString() {
        return "WeatherZone[" + this.southwest + ", " + this.northeast + ", " + this.altitudeBand + "]";
    }
}