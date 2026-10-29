package eapli.aisafe.aircontrolareamanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeographicBoundaryTest {

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryRejectsNullSouthwest() {
		new GeographicBoundary(null, new Coordinates(10.0, 10.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryRejectsNullNortheast() {
		new GeographicBoundary(new Coordinates(0.0, 0.0), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryRejectsZeroArea() {
		new GeographicBoundary(new Coordinates(10.0, 10.0), new Coordinates(10.0, 10.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryRejectsInvertedLatitude() {
		new GeographicBoundary(new Coordinates(20.0, 0.0), new Coordinates(10.0, 10.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryRejectsInvertedLongitude() {
		new GeographicBoundary(new Coordinates(0.0, 20.0), new Coordinates(10.0, 10.0));
	}

	@Test
	public void ensureBoundaryAcceptsValidRectangle() {
		new GeographicBoundary(new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
	}

	@Test
	public void ensureBoundaryContainsPointInsideRectangle() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertTrue(boundary.contains(new Coordinates(5.0, 5.0)));
	}

	@Test
	public void ensureBoundaryDoesNotContainPointOutside() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(boundary.contains(new Coordinates(20.0, 20.0)));
	}

	@Test
	public void ensureBoundaryDoesNotContainPointSouthOfRectangle() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(boundary.contains(new Coordinates(-5.0, 5.0)));
	}

	@Test
	public void ensureBoundaryDoesNotContainPointNorthOfRectangle() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(boundary.contains(new Coordinates(15.0, 5.0)));
	}

	@Test
	public void ensureBoundaryDoesNotContainPointWestOfRectangle() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(boundary.contains(new Coordinates(5.0, -5.0)));
	}

	@Test
	public void ensureBoundaryDoesNotContainPointEastOfRectangle() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(boundary.contains(new Coordinates(5.0, 15.0)));
	}

	@Test
	public void ensureBoundaryContainsPointOnEdge() {
		final GeographicBoundary boundary = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertTrue(boundary.contains(new Coordinates(0.0, 5.0)));
		assertTrue(boundary.contains(new Coordinates(10.0, 5.0)));
		assertTrue(boundary.contains(new Coordinates(5.0, 0.0)));
		assertTrue(boundary.contains(new Coordinates(5.0, 10.0)));
	}

	@Test
	public void ensureBoundaryOverlapsWhenRectanglesIntersect() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(5.0, 5.0), new Coordinates(15.0, 15.0));

		assertTrue(one.overlaps(another));
	}

	@Test
	public void ensureBoundaryDoesNotOverlapDisjointRectangles() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(20.0, 20.0), new Coordinates(30.0, 30.0));

		assertFalse(one.overlaps(another));
	}

	@Test
	public void ensureBoundaryOverlapsWhenSharingEdges() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(0.0, 10.0), new Coordinates(10.0, 20.0));

		assertTrue(one.overlaps(another));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryContainsRejectsNullPoint() {
		new GeographicBoundary(new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0))
				.contains(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureBoundaryOverlapsRejectsNullOther() {
		new GeographicBoundary(new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0))
				.overlaps(null);
	}

	@Test
	public void ensureBoundaryExposesSouthwestAndNortheast() {
		final Coordinates sw = new Coordinates(0.0, 0.0);
		final Coordinates ne = new Coordinates(10.0, 10.0);
		final GeographicBoundary b = new GeographicBoundary(sw, ne);

		assertEquals(sw, b.southwest());
		assertEquals(ne, b.northeast());
	}

	@Test
	public void ensureBoundaryDoesNotOverlapEastNeighbor() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary east = new GeographicBoundary(
				new Coordinates(0.0, 20.0), new Coordinates(10.0, 30.0));

		assertFalse(one.overlaps(east));
	}

	@Test
	public void ensureBoundaryDoesNotOverlapWestNeighbor() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary west = new GeographicBoundary(
				new Coordinates(0.0, -20.0), new Coordinates(10.0, -10.0));

		assertFalse(one.overlaps(west));
	}

	@Test
	public void ensureBoundaryDoesNotOverlapNorthNeighbor() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary north = new GeographicBoundary(
				new Coordinates(20.0, 0.0), new Coordinates(30.0, 10.0));

		assertFalse(one.overlaps(north));
	}

	@Test
	public void ensureBoundaryDoesNotOverlapSouthNeighbor() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(10.0, 0.0), new Coordinates(20.0, 10.0));
		final GeographicBoundary south = new GeographicBoundary(
				new Coordinates(-10.0, 0.0), new Coordinates(0.0, 10.0));

		assertFalse(one.overlaps(south));
	}

	@Test
	public void ensureBoundaryEqualsSelf() {
		final GeographicBoundary b = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertTrue(b.equals(b));
	}

	@Test
	public void ensureBoundaryEqualsNullReturnsFalse() {
		final GeographicBoundary b = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(b.equals(null));
	}

	@Test
	public void ensureBoundaryEqualsOtherTypeReturnsFalse() {
		final GeographicBoundary b = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertFalse(b.equals("not a boundary"));
	}

	@Test
	public void ensureBoundaryEqualsPassesForSameCorners() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertTrue(one.equals(another));
	}

	@Test
	public void ensureBoundaryEqualsFailsForDifferentCorners() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(20.0, 20.0));

		assertFalse(one.equals(another));
	}

	@Test
	public void ensureBoundaryEqualsFailsForDifferentSouthwest() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(1.0, 1.0), new Coordinates(10.0, 10.0));

		assertFalse(one.equals(another));
	}

	@Test
	public void ensureBoundaryHashCodeConsistentForEqualBoundaries() {
		final GeographicBoundary one = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
		final GeographicBoundary another = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));

		assertEquals(one.hashCode(), another.hashCode());
	}

	@Test
	public void ensureBoundaryToStringContainsBothCorners() {
		final String text = new GeographicBoundary(
				new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0)).toString();

		assertTrue(text.contains("0.0"));
		assertTrue(text.contains("10.0"));
	}
}