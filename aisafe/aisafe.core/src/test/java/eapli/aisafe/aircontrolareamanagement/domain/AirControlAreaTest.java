package eapli.aisafe.aircontrolareamanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AirControlAreaTest {

	private GeographicBoundary validBoundary() {
		return new GeographicBoundary(new Coordinates(0.0, 0.0), new Coordinates(10.0, 10.0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAirControlAreaRejectsNullCode() {
		new AirControlArea(null, new AreaName("Lisbon TMA"), validBoundary());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAirControlAreaRejectsNullName() {
		new AirControlArea(new AreaCode("LPPC"), null, validBoundary());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAirControlAreaRejectsNullBoundary() {
		new AirControlArea(new AreaCode("LPPC"), new AreaName("Lisbon TMA"), null);
	}

	@Test
	public void ensureAirControlAreaAcceptsValidInput() {
		final AreaCode code = new AreaCode("LPPC");
		final AreaName name = new AreaName("Lisbon TMA");
		final GeographicBoundary boundary = validBoundary();

		final AirControlArea area = new AirControlArea(code, name, boundary);

		assertEquals(code, area.identity());
		assertEquals(name, area.name());
		assertEquals(boundary, area.boundary());
	}

	@Test
	public void ensureAirControlAreaEqualsPassesForSameCode() {
		final AirControlArea one = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());
		final AirControlArea another = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Other Name"),
				new GeographicBoundary(new Coordinates(20.0, 20.0), new Coordinates(30.0, 30.0)));

		assertTrue(one.equals(another));
	}

	@Test
	public void ensureAirControlAreaEqualsFailsForDifferentCode() {
		final AirControlArea one = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());
		final AirControlArea another = new AirControlArea(
				new AreaCode("LPPO"), new AreaName("Lisbon TMA"), validBoundary());

		assertFalse(one.equals(another));
	}

	@Test
	public void ensureAirControlAreaCodeAliasesIdentity() {
		final AreaCode code = new AreaCode("LPPC");
		final AirControlArea area = new AirControlArea(
				code, new AreaName("Lisbon TMA"), validBoundary());

		assertEquals(area.identity(), area.code());
	}

	@Test
	public void ensureAirControlAreaHashCodeConsistentWithEquality() {
		final AirControlArea one = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());
		final AirControlArea another = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Other"),
				new GeographicBoundary(new Coordinates(20.0, 20.0), new Coordinates(30.0, 30.0)));

		assertEquals(one.hashCode(), another.hashCode());
	}

	@Test
	public void ensureAirControlAreaSameAsTracksEquality() {
		final AirControlArea one = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());
		final AirControlArea another = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());

		assertTrue(one.sameAs(another));
	}

	@Test
	public void ensureAirControlAreaSameAsFailsForDifferentCode() {
		final AirControlArea one = new AirControlArea(
				new AreaCode("LPPC"), new AreaName("Lisbon TMA"), validBoundary());
		final AirControlArea another = new AirControlArea(
				new AreaCode("LPPO"), new AreaName("Lisbon TMA"), validBoundary());

		assertFalse(one.sameAs(another));
	}
}