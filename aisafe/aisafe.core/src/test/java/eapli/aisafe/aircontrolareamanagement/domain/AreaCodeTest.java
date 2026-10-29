package eapli.aisafe.aircontrolareamanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AreaCodeTest {

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaCodeRejectsNullValue() {
		new AreaCode(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaCodeRejectsEmptyValue() {
		new AreaCode("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaCodeRejectsBlankValue() {
		new AreaCode("   ");
	}

	@Test
	public void ensureAreaCodeAcceptsValidValue() {
		final AreaCode code = new AreaCode("LPPC");

		assertEquals("LPPC", code.value());
	}

	@Test
	public void ensureAreaCodeEqualsPassesForSameValue() {
		final AreaCode one = new AreaCode("LPPC");
		final AreaCode another = new AreaCode("LPPC");

		assertTrue(one.equals(another));
	}

	@Test
	public void ensureAreaCodeEqualsFailsForDifferentValue() {
		final AreaCode one = new AreaCode("LPPC");
		final AreaCode another = new AreaCode("LPPO");

		assertFalse(one.equals(another));
	}

	@Test
	public void ensureAreaCodeEqualsSelf() {
		final AreaCode c = new AreaCode("LPPC");

		assertTrue(c.equals(c));
	}

	@Test
	public void ensureAreaCodeEqualsNullReturnsFalse() {
		assertFalse(new AreaCode("LPPC").equals(null));
	}

	@Test
	public void ensureAreaCodeEqualsOtherTypeReturnsFalse() {
		assertFalse(new AreaCode("LPPC").equals("LPPC"));
	}

	@Test
	public void ensureAreaCodeHashCodeConsistentForEqualCodes() {
		assertEquals(new AreaCode("LPPC").hashCode(), new AreaCode("LPPC").hashCode());
	}

	@Test
	public void ensureAreaCodeToStringReturnsValue() {
		assertEquals("LPPC", new AreaCode("LPPC").toString());
	}

	@Test
	public void ensureAreaCodeValueOfMatchesConstructor() {
		assertEquals(new AreaCode("LPPC"), AreaCode.valueOf("LPPC"));
	}

	@Test
	public void ensureAreaCodeCompareToOrdersLexicographically() {
		final AreaCode lppc = new AreaCode("LPPC");
		final AreaCode lppo = new AreaCode("LPPO");

		assertTrue(lppc.compareTo(lppo) < 0);
		assertTrue(lppo.compareTo(lppc) > 0);
		assertEquals(0, lppc.compareTo(new AreaCode("LPPC")));
	}
}