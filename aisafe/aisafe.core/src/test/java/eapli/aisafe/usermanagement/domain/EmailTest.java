package eapli.aisafe.usermanagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmailTest {

	@Test
	void shouldCreateEmailWithAisafeDomain() {
		final Email email = Email.valueOf("john.doe@aisafe.pt");
		assertEquals("john.doe@aisafe.pt", email.toString());
	}

	@Test
	void shouldRejectEmailWithInvalidFormat() {
		assertThrows(IllegalArgumentException.class, () -> Email.valueOf("john.doe-aisafe.pt"));
	}

	@Test
	void shouldCreateEmailWithExternalDomain() {
		final Email email = Email.valueOf("john.doe@gmail.com");
		assertEquals("john.doe@gmail.com", email.toString());
	}
}
