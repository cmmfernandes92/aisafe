package eapli.aisafe.aircontrolareamanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AreaNameTest {

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaNameRejectsNullValue() {
		new AreaName(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaNameRejectsEmptyValue() {
		new AreaName("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaNameRejectsBlankValue() {
		new AreaName("   ");
	}

	@Test
	public void ensureAreaNameAcceptsValidValue() {
		final AreaName name = new AreaName("Lisbon TMA");

		assertEquals("Lisbon TMA", name.value());
	}

	@Test
	public void ensureAreaNameEqualsPassesForSameValue() {
		final AreaName one = new AreaName("Lisbon TMA");
		final AreaName another = new AreaName("Lisbon TMA");

		assertTrue(one.equals(another));
	}

	@Test
	public void ensureAreaNameEqualsFailsForDifferentValue() {
		final AreaName one = new AreaName("Lisbon TMA");
		final AreaName another = new AreaName("Porto TMA");

		assertFalse(one.equals(another));
	}

	@Test
	public void ensureAreaNameEqualsSelf() {
		final AreaName n = new AreaName("Lisbon TMA");

		assertTrue(n.equals(n));
	}

	@Test
	public void ensureAreaNameEqualsNullReturnsFalse() {
		assertFalse(new AreaName("Lisbon TMA").equals(null));
	}

	@Test
	public void ensureAreaNameEqualsOtherTypeReturnsFalse() {
		assertFalse(new AreaName("Lisbon TMA").equals("Lisbon TMA"));
	}

	@Test
	public void ensureAreaNameHashCodeConsistentForEqualNames() {
		assertEquals(new AreaName("Lisbon TMA").hashCode(),
				new AreaName("Lisbon TMA").hashCode());
	}

	@Test
	public void ensureAreaNameToStringReturnsValue() {
		assertEquals("Lisbon TMA", new AreaName("Lisbon TMA").toString());
	}

	@Test
	public void ensureAreaNameValueOfMatchesConstructor() {
		assertEquals(new AreaName("Lisbon TMA"), AreaName.valueOf("Lisbon TMA"));
	}

	@Test
	public void ensureAreaNameCompareToOrdersLexicographically() {
		final AreaName lisbon = new AreaName("Lisbon TMA");
		final AreaName porto = new AreaName("Porto TMA");

		assertTrue(lisbon.compareTo(porto) < 0);
		assertTrue(porto.compareTo(lisbon) > 0);
		assertEquals(0, lisbon.compareTo(new AreaName("Lisbon TMA")));
	}
}