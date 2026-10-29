package eapli.aisafe.usermanagement.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void shouldRejectExpiredSecurityClearanceDate() {
		assertThrows(IllegalArgumentException.class, () -> SecurityClearance.valueOf(LocalDate.now().minusDays(1)));
	}

	@Test
	void shouldRejectSkillsAssessmentOlderThanFiveYears() {
		assertThrows(IllegalArgumentException.class, () -> SkillsAssessment.valueOf(LocalDate.now().minusYears(6)));
	}
}
