package eapli.aisafe.aircontrolareamanagement.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class Coordinates implements ValueObject {

	private static final long serialVersionUID = 1L;

	private double latitude;
	private double longitude;

	public Coordinates(final double latitude, final double longitude) {
		if (Double.isNaN(latitude) || Double.isInfinite(latitude)) {
			throw new IllegalArgumentException("Latitude must be a finite number");
		}
		if (latitude < -90.0 || latitude > 90.0) {
			throw new IllegalArgumentException("Latitude must be in [-90, 90]");
		}
		if (Double.isNaN(longitude) || Double.isInfinite(longitude)) {
			throw new IllegalArgumentException("Longitude must be a finite number");
		}
		if (longitude < -180.0 || longitude > 180.0) {
			throw new IllegalArgumentException("Longitude must be in [-180, 180]");
		}
		this.latitude = latitude;
		this.longitude = longitude;
	}

	protected Coordinates() {
		// for ORM
	}

	public double latitude() {
		return this.latitude;
	}

	public double longitude() {
		return this.longitude;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Coordinates)) {
			return false;
		}
		final Coordinates that = (Coordinates) o;
		return Double.compare(this.latitude, that.latitude) == 0
				&& Double.compare(this.longitude, that.longitude) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.latitude, this.longitude);
	}

	@Override
	public String toString() {
		return "(" + this.latitude + ", " + this.longitude + ")";
	}
}