package eapli.aisafe.aircontrolareamanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoordinatesTest {

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsNaNLatitude() {
		new Coordinates(Double.NaN, 0.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsInfiniteLatitude() {
		new Coordinates(Double.POSITIVE_INFINITY, 0.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsNaNLongitude() {
		new Coordinates(0.0, Double.NaN);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsInfiniteLongitude() {
		new Coordinates(0.0, Double.NEGATIVE_INFINITY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsLatitudeBelowMinusNinety() {
		new Coordinates(-90.01, 0.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsLatitudeAboveNinety() {
		new Coordinates(90.01, 0.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsLongitudeBelowMinusOneEighty() {
		new Coordinates(0.0, -180.01);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCoordinatesRejectsLongitudeAboveOneEighty() {
		new Coordinates(0.0, 180.01);
	}

	@Test
	public void ensureCoordinatesAcceptsBoundaryLatitudeValues() {
		new Coordinates(-90.0, 0.0);
		new Coordinates(90.0, 0.0);
	}

	@Test
	public void ensureCoordinatesAcceptsBoundaryLongitudeValues() {
		new Coordinates(0.0, -180.0);
		new Coordinates(0.0, 180.0);
	}

	@Test
	public void ensureCoordinatesAcceptsValidValues() {
		final Coordinates coords = new Coordinates(38.7223, -9.1393);

		assertEquals(38.7223, coords.latitude(), 0.0);
		assertEquals(-9.1393, coords.longitude(), 0.0);
	}

	@Test
	public void ensureCoordinatesEqualsSelf() {
		final Coordinates c = new Coordinates(10.0, 20.0);

		assertTrue(c.equals(c));
	}

	@Test
	public void ensureCoordinatesEqualsNullReturnsFalse() {
		assertFalse(new Coordinates(10.0, 20.0).equals(null));
	}

	@Test
	public void ensureCoordinatesEqualsOtherTypeReturnsFalse() {
		assertFalse(new Coordinates(10.0, 20.0).equals("not a coordinate"));
	}

	@Test
	public void ensureCoordinatesEqualsPassesForSameValues() {
		final Coordinates one = new Coordinates(10.0, 20.0);
		final Coordinates another = new Coordinates(10.0, 20.0);

		assertTrue(one.equals(another));
	}

	@Test
	public void ensureCoordinatesEqualsFailsForDifferentLatitude() {
		assertFalse(new Coordinates(10.0, 20.0).equals(new Coordinates(11.0, 20.0)));
	}

	@Test
	public void ensureCoordinatesEqualsFailsForDifferentLongitude() {
		assertFalse(new Coordinates(10.0, 20.0).equals(new Coordinates(10.0, 21.0)));
	}

	@Test
	public void ensureCoordinatesHashCodeConsistentForEqualValues() {
		assertEquals(new Coordinates(10.0, 20.0).hashCode(),
				new Coordinates(10.0, 20.0).hashCode());
	}

	@Test
	public void ensureCoordinatesHashCodeDiffersForDifferentValues() {
		assertNotEquals(new Coordinates(10.0, 20.0).hashCode(),
				new Coordinates(15.0, 20.0).hashCode());
	}

	@Test
	public void ensureCoordinatesToStringContainsBothValues() {
		final String text = new Coordinates(10.0, 20.0).toString();

		assertTrue(text.contains("10.0"));
		assertTrue(text.contains("20.0"));
	}
}