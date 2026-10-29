package eapli.aisafe.usermanagement.domain;

import java.util.Set;

import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.validations.Preconditions;

public final class EmailDomainPolicy {

	private static final Set<String> APPROVED_INTERNAL_DOMAINS = Set.of("@aisafe.pt");
	private static final Set<Role> INTERNAL_ROLES = Set.of(
			AisafeRoles.POWER_USER,
			AisafeRoles.ADMIN,
			AisafeRoles.BACKOFFICE_OPERATOR,
			AisafeRoles.WEATHER_PERSON);

	private EmailDomainPolicy() {
	}

	public static void ensureAllowedForRoles(final Email email, final Set<Role> roles) {
		Preconditions.noneNull(email, roles);
		if (!hasInternalRole(roles)) {
			return;
		}
		final String normalizedEmail = email.toString().toLowerCase();
		if (APPROVED_INTERNAL_DOMAINS.stream().noneMatch(normalizedEmail::endsWith)) {
			throw new IllegalArgumentException("Internal user email must belong to an approved AISafe domain");
		}
	}

	public static boolean hasInternalRole(final Set<Role> roles) {
		Preconditions.nonNull(roles);
		return roles.stream().anyMatch(INTERNAL_ROLES::contains);
	}
}
