package eapli.aisafe.aircontrolareamanagement.domain;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class GeographicBoundary implements ValueObject {

	private static final long serialVersionUID = 1L;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "latitude", column = @Column(name = "sw_latitude")),
			@AttributeOverride(name = "longitude", column = @Column(name = "sw_longitude"))
	})
	private Coordinates southwest;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "latitude", column = @Column(name = "ne_latitude")),
			@AttributeOverride(name = "longitude", column = @Column(name = "ne_longitude"))
	})
	private Coordinates northeast;

	public GeographicBoundary(final Coordinates southwest, final Coordinates northeast) {
		if (southwest == null) {
			throw new IllegalArgumentException("Southwest coordinate must not be null");
		}
		if (northeast == null) {
			throw new IllegalArgumentException("Northeast coordinate must not be null");
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
	}

	protected GeographicBoundary() {
		// for ORM
	}

	public Coordinates southwest() {
		return this.southwest;
	}

	public Coordinates northeast() {
		return this.northeast;
	}

	public boolean contains(final Coordinates point) {
		if (point == null) {
			throw new IllegalArgumentException("Point must not be null");
		}
		return point.latitude() >= this.southwest.latitude()
				&& point.latitude() <= this.northeast.latitude()
				&& point.longitude() >= this.southwest.longitude()
				&& point.longitude() <= this.northeast.longitude();
	}

	public boolean overlaps(final GeographicBoundary other) {
		if (other == null) {
			throw new IllegalArgumentException("Other boundary must not be null");
		}
		final boolean disjoint =
				this.northeast.longitude() < other.southwest.longitude()
				|| this.southwest.longitude() > other.northeast.longitude()
				|| this.northeast.latitude() < other.southwest.latitude()
				|| this.southwest.latitude() > other.northeast.latitude();
		return !disjoint;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof GeographicBoundary)) {
			return false;
		}
		final GeographicBoundary that = (GeographicBoundary) o;
		return this.southwest.equals(that.southwest) && this.northeast.equals(that.northeast);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.southwest, this.northeast);
	}

	@Override
	public String toString() {
		return "GeographicBoundary[" + this.southwest + ", " + this.northeast + "]";
	}
}