package eapli.aisafe.aircontrolareamanagement.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;

@Entity
public class AirControlArea implements AggregateRoot<AreaCode> {

	private static final long serialVersionUID = 1L;

	@Version
	private Long version;

	@EmbeddedId
	@AttributeOverride(name = "value", column = @Column(name = "area_code"))
	private AreaCode code;

	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "area_name"))
	private AreaName name;

	@Embedded
	private GeographicBoundary boundary;

	public AirControlArea(final AreaCode code, final AreaName name, final GeographicBoundary boundary) {
		if (code == null) {
			throw new IllegalArgumentException("Area code must not be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("Area name must not be null");
		}
		if (boundary == null) {
			throw new IllegalArgumentException("Geographic boundary must not be null");
		}
		this.code = code;
		this.name = name;
		this.boundary = boundary;
	}

	protected AirControlArea() {
		// for ORM
	}

	@Override
	public AreaCode identity() {
		return this.code;
	}

	public AreaCode code() {
		return identity();
	}

	public AreaName name() {
		return this.name;
	}

	public GeographicBoundary boundary() {
		return this.boundary;
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
}