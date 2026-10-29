package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;
import eapli.framework.strings.util.StringPredicates;

public class AisafePasswordPolicy implements PasswordPolicy {

	@Override
	public boolean isSatisfiedBy(final String rawPassword) {
		if (StringPredicates.isNullOrEmpty(rawPassword) || (rawPassword.length() < 6)
				|| !StringPredicates.containsDigit(rawPassword)) {
			return false;
		}
		return StringPredicates.containsCapital(rawPassword);
	}
}
