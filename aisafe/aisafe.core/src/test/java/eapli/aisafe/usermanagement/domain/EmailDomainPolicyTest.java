package eapli.aisafe.usermanagement.domain;

import java.util.Set;

import org.junit.Test;

public class EmailDomainPolicyTest {

	@Test
	public void ensureInternalRoleAcceptsApprovedDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("operator@aisafe.pt"),
				Set.of(AisafeRoles.BACKOFFICE_OPERATOR));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureInternalRoleRejectsUnapprovedDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("operator@gmail.com"),
				Set.of(AisafeRoles.BACKOFFICE_OPERATOR));
	}

	@Test
	public void ensureExternalAtccRoleAcceptsAnyValidDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("personal@gmail.com"),
				Set.of(AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR));
	}

	@Test
	public void ensureExternalFcoRoleAcceptsAnyValidDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("controller@nav.pt"),
				Set.of(AisafeRoles.FLIGHT_CONTROL_OPERATOR));
	}

	@Test
	public void ensureExternalPilotRoleAcceptsAnyValidDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("pilot@hotmail.com"),
				Set.of(AisafeRoles.PILOT));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureMixedInternalAndExternalRolesStillRequireApprovedDomain() {
		EmailDomainPolicy.ensureAllowedForRoles(Email.valueOf("mixed@gmail.com"),
				Set.of(AisafeRoles.BACKOFFICE_OPERATOR, AisafeRoles.PILOT));
	}
}
