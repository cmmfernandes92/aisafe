package eapli.aisafe.weather.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class WeatherData implements AggregateRoot<WeatherDataId> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private WeatherDataId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = false))
    private AreaCode areaCode;

    @Embedded
    private TimePeriod timePeriod;

    @Embedded
    private WeatherZone zone;

    @Column(nullable = true)
    private Double temperature;

    @Column(nullable = true)
    private Double pressure;

    @Column(nullable = true)
    private Double visibility;

    @Embedded
    private WeatherWindSpeed windSpeed;

    @Embedded
    private WeatherWindDirection windDirection;

    @Embedded
    private DataSource dataSource;

    /**
     * Wind-only record: temperature, pressure and visibility are absent. Wind is
     * the only measurement the spec ties to flight data, so it is mandatory; the
     * others are optional and supplied by sources that carry them.
     */
    public WeatherData(final AreaCode areaCode, final TimePeriod timePeriod, final WeatherZone zone,
            final WeatherWindSpeed windSpeed, final WeatherWindDirection windDirection,
            final DataSource dataSource) {
        this(areaCode, timePeriod, zone, null, null, null, windSpeed, windDirection, dataSource);
    }

    public WeatherData(final AreaCode areaCode, final TimePeriod timePeriod, final WeatherZone zone,
            final Double temperature, final Double pressure, final Double visibility,
            final WeatherWindSpeed windSpeed, final WeatherWindDirection windDirection,
            final DataSource dataSource) {
        Preconditions.noneNull(areaCode, timePeriod, zone, windSpeed, windDirection, dataSource);
        if (pressure != null && pressure <= 0) {
            throw new IllegalArgumentException("Atmospheric pressure must be positive");
        }
        if (visibility != null && visibility < 0) {
            throw new IllegalArgumentException("Visibility must be non-negative");
        }
        this.id = WeatherDataId.newId();
        this.areaCode = areaCode;
        this.timePeriod = timePeriod;
        this.zone = zone;
        this.temperature = temperature;
        this.pressure = pressure;
        this.visibility = visibility;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.dataSource = dataSource;
    }

    protected WeatherData() {
        // for ORM
    }

    @Override
    public WeatherDataId identity() {
        return this.id;
    }

    public AreaCode areaCode() {
        return this.areaCode;
    }

    public TimePeriod timePeriod() {
        return this.timePeriod;
    }

    public WeatherZone zone() {
        return this.zone;
    }

    /** @return temperature in Celsius, or {@code null} if the source omits it. */
    public Double temperature() {
        return this.temperature;
    }

    /** @return atmospheric pressure, or {@code null} if the source omits it. */
    public Double pressure() {
        return this.pressure;
    }

    /** @return visibility, or {@code null} if the source omits it. */
    public Double visibility() {
        return this.visibility;
    }

    public WeatherWindSpeed windSpeed() {
        return this.windSpeed;
    }

    public WeatherWindDirection windDirection() {
        return this.windDirection;
    }

    public DataSource dataSource() {
        return this.dataSource;
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
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public String toString() {
        return areaCode + " [" + timePeriod + "] src=" + dataSource;
    }
}
