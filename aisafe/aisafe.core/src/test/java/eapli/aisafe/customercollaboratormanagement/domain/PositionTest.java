package eapli.aisafe.customercollaboratormanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PositionTest {

	@Test
	public void ensurePositionCanBeCreatedForValidValue() {
		final Position position = Position.valueOf("  Operations Manager  ");

		assertEquals("Operations Manager", position.value());
		assertEquals("Operations Manager", position.toString());
	}

	@Test
	public void ensurePositionEqualityUsesValue() {
		final Position position = Position.valueOf("Operations Manager");
		final Position same = Position.valueOf("Operations Manager");
		final Position different = Position.valueOf("Flight Controller");

		assertTrue(position.equals(position));
		assertTrue(position.equals(same));
		assertEquals(position.hashCode(), same.hashCode());
		assertFalse(position.equals(different));
		assertFalse(position.equals("Operations Manager"));
	}

	@Test
	public void ensurePositionCanBeCompared() {
		final Position first = Position.valueOf("A");
		final Position second = Position.valueOf("B");

		assertTrue(first.compareTo(second) < 0);
		assertEquals(0, first.compareTo(Position.valueOf("A")));
		assertNotEquals(first, second);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensurePositionMustNotBeBlankForCreation() {
		Position.valueOf("   ");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensurePositionMustNotBeNullForCreation() {
		Position.valueOf(null);
	}
}
