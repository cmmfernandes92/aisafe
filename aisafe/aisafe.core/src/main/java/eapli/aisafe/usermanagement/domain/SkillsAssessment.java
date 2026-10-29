package eapli.aisafe.usermanagement.domain;

import java.time.LocalDate;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class SkillsAssessment implements ValueObject {

	private static final long serialVersionUID = 1L;

	private LocalDate lastAssessmentDate;

	public SkillsAssessment(final LocalDate lastAssessmentDate) {
		if (lastAssessmentDate == null) {
			throw new IllegalArgumentException("Skills assessment date is mandatory");
		}
		if (lastAssessmentDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Skills assessment date cannot be in the future");
		}
		if (lastAssessmentDate.isBefore(LocalDate.now().minusYears(5))) {
			throw new IllegalArgumentException("Skills assessment date cannot be older than 5 years");
		}
		this.lastAssessmentDate = lastAssessmentDate;
	}

	protected SkillsAssessment() {
		// for ORM
	}

	public static SkillsAssessment valueOf(final LocalDate lastAssessmentDate) {
		return new SkillsAssessment(lastAssessmentDate);
	}

	public LocalDate lastAssessmentDate() {
		return this.lastAssessmentDate;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SkillsAssessment)) {
			return false;
		}
		final SkillsAssessment that = (SkillsAssessment) o;
		return this.lastAssessmentDate.equals(that.lastAssessmentDate);
	}

	@Override
	public int hashCode() {
		return this.lastAssessmentDate.hashCode();
	}
}
