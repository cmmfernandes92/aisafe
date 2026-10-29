package eapli.aisafe.airportmanagement.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class Airport implements AggregateRoot<AirportICAOCode> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "icao_code"))
    private AirportICAOCode icaoCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "iata_code", nullable = false, unique = true))
    private AirportIATACode iataCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "airport_name", nullable = false))
    private AirportName airportName;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "town", nullable = false))
    private Town town;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "elevation_meters", nullable = false))
    private Elevation elevation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "airport_latitude", nullable = false)),
            @AttributeOverride(name = "longitude", column = @Column(name = "airport_longitude", nullable = false))
    })
    private Coordinates coordinates;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "country_code", nullable = false))
    private CountryCode countryCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = false))
    private AreaCode areaCode;

    public Airport(final AirportICAOCode icaoCode, final AirportIATACode iataCode,
            final AirportName airportName, final Town town, final Elevation elevation,
            final Coordinates coordinates, final CountryCode countryCode, final AreaCode areaCode) {
        Preconditions.noneNull(icaoCode, iataCode, airportName, town, elevation, coordinates,
                countryCode, areaCode);
        this.icaoCode = icaoCode;
        this.iataCode = iataCode;
        this.airportName = airportName;
        this.town = town;
        this.elevation = elevation;
        this.coordinates = coordinates;
        this.countryCode = countryCode;
        this.areaCode = areaCode;
    }

    protected Airport() {
        // for ORM
    }

    @Override
    public AirportICAOCode identity() {
        return this.icaoCode;
    }

    public AirportICAOCode icaoCode() {
        return identity();
    }

    public AirportIATACode iataCode() {
        return this.iataCode;
    }

    public AirportName airportName() {
        return this.airportName;
    }

    public Town town() {
        return this.town;
    }

    public Elevation elevation() {
        return this.elevation;
    }

    public Coordinates coordinates() {
        return this.coordinates;
    }

    public CountryCode countryCode() {
        return this.countryCode;
    }

    public AreaCode areaCode() {
        return this.areaCode;
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
        return this.icaoCode + " / " + this.iataCode + " - " + this.airportName;
    }
}
