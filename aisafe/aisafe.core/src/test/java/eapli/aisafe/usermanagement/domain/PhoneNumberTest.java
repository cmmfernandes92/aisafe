package eapli.aisafe.usermanagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PhoneNumberTest {

	@Test
	void shouldAcceptInternationalPhoneNumber() {
		final PhoneNumber phoneNumber = PhoneNumber.valueOf("+351912345678");
		assertEquals("+351912345678", phoneNumber.number());
	}

	@Test
	void shouldRejectPhoneWithoutCountryPrefix() {
		assertThrows(IllegalArgumentException.class, () -> PhoneNumber.valueOf("912345678"));
	}

	@Test
	void shouldRejectPhoneWithInvalidCharacters() {
		assertThrows(IllegalArgumentException.class, () -> PhoneNumber.valueOf("+35191A345678"));
	}
}
