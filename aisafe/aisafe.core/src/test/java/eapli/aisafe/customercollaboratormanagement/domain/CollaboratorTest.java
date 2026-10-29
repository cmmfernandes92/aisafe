package eapli.aisafe.customercollaboratormanagement.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;

public class CollaboratorTest {

	@Test
	public void ensureNewCollaboratorDefaultsToActiveStatus() {
		final Email email = Email.valueOf("x@y.pt");
		final Position position = Position.valueOf("Officer");
		final PhoneNumber phoneNumber = PhoneNumber.valueOf("+351910000000");

		final CompanyCollaborator companyCollaborator = new CompanyCollaborator(email, position,
				CompanyIATACode.valueOf("TP"), email, phoneNumber);
		final AreaCollaborator areaCollaborator = new AreaCollaborator(email, position, AreaCode.valueOf("LPPC"),
				email, phoneNumber);

		assertEquals(CollaboratorStatus.ACTIVE, companyCollaborator.status());
		assertEquals(CollaboratorStatus.ACTIVE, areaCollaborator.status());
	}

	@Test
	public void ensureCompanyCollaboratorExposesIdentityPositionAndCompanyCode() {
		final Email email = Email.valueOf("company@y.pt");
		final Position position = Position.valueOf("Officer");
		final CompanyIATACode companyCode = CompanyIATACode.valueOf("TP");
		final PhoneNumber phoneNumber = PhoneNumber.valueOf("+351910000001");

		final CompanyCollaborator collaborator = new CompanyCollaborator(email, position, companyCode, email,
				phoneNumber);

		assertEquals(email, collaborator.identity());
		assertEquals(email, collaborator.email());
		assertEquals(position, collaborator.position());
		assertEquals(companyCode, collaborator.companyCode());
		assertEquals(email, collaborator.contactEmail());
		assertEquals(phoneNumber, collaborator.contactPhoneNumber());
		assertTrue(collaborator.isActive());
	}

	@Test
	public void ensureAreaCollaboratorExposesIdentityPositionAndAreaCode() {
		final Email email = Email.valueOf("area@y.pt");
		final Position position = Position.valueOf("FCO");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		final PhoneNumber phoneNumber = PhoneNumber.valueOf("+351910000002");

		final AreaCollaborator collaborator = new AreaCollaborator(email, position, areaCode, email, phoneNumber);

		assertEquals(email, collaborator.identity());
		assertEquals(email, collaborator.email());
		assertEquals(position, collaborator.position());
		assertEquals(areaCode, collaborator.areaCode());
		assertEquals(email, collaborator.contactEmail());
		assertEquals(phoneNumber, collaborator.contactPhoneNumber());
		assertTrue(collaborator.isActive());
	}

	@Test
	public void ensureCollaboratorContactDetailsCanChangeWithoutChangingIdentity() {
		final Email identityEmail = Email.valueOf("login@y.pt");
		final CompanyCollaborator collaborator = new CompanyCollaborator(identityEmail, Position.valueOf("Officer"),
				CompanyIATACode.valueOf("TP"), identityEmail, PhoneNumber.valueOf("+351910000003"));
		final Email newContactEmail = Email.valueOf("contact@gmail.com");
		final PhoneNumber newContactPhone = PhoneNumber.valueOf("+351919999999");

		collaborator.changeContactDetails(newContactEmail, newContactPhone);

		assertEquals(identityEmail, collaborator.identity());
		assertEquals(newContactEmail, collaborator.contactEmail());
		assertEquals(newContactPhone, collaborator.contactPhoneNumber());
	}

	@Test
	public void ensureCollaboratorCanBeDisabled() {
		final CompanyCollaborator collaborator = new CompanyCollaborator(Email.valueOf("disable@y.pt"),
				Position.valueOf("Officer"), CompanyIATACode.valueOf("TP"), Email.valueOf("disable@y.pt"),
				PhoneNumber.valueOf("+351910000004"));

		collaborator.disable();

		assertEquals(CollaboratorStatus.DISABLED, collaborator.status());
		assertFalse(collaborator.isActive());
	}

	@Test(expected = IllegalStateException.class)
	public void ensureCollaboratorCannotBeDisabledTwice() {
		final AreaCollaborator collaborator = new AreaCollaborator(Email.valueOf("disabled@y.pt"),
				Position.valueOf("FCO"), AreaCode.valueOf("LPPC"), Email.valueOf("disabled@y.pt"),
				PhoneNumber.valueOf("+351910000005"));

		collaborator.disable();
		collaborator.disable();
	}

	@Test
	public void ensureCollaboratorEqualityUsesIdentity() {
		final CompanyCollaborator collaborator = new CompanyCollaborator(Email.valueOf("same@y.pt"),
				Position.valueOf("Officer"), CompanyIATACode.valueOf("TP"), Email.valueOf("same@y.pt"),
				PhoneNumber.valueOf("+351910000006"));
		final CompanyCollaborator sameIdentity = new CompanyCollaborator(Email.valueOf("same@y.pt"),
				Position.valueOf("Different"), CompanyIATACode.valueOf("FR"), Email.valueOf("alt@y.pt"),
				PhoneNumber.valueOf("+351910000007"));
		final CompanyCollaborator differentIdentity = new CompanyCollaborator(Email.valueOf("different@y.pt"),
				Position.valueOf("Officer"), CompanyIATACode.valueOf("TP"), Email.valueOf("different@y.pt"),
				PhoneNumber.valueOf("+351910000008"));

		assertTrue(collaborator.sameAs(sameIdentity));
		assertTrue(collaborator.equals(sameIdentity));
		assertEquals(collaborator.hashCode(), sameIdentity.hashCode());
		assertFalse(collaborator.sameAs(differentIdentity));
		assertNotEquals(collaborator, differentIdentity);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCompanyCollaboratorRejectsNullEmail() {
		new CompanyCollaborator(null, Position.valueOf("Officer"), CompanyIATACode.valueOf("TP"),
				Email.valueOf("contact@y.pt"), PhoneNumber.valueOf("+351910000009"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCompanyCollaboratorRejectsNullPosition() {
		new CompanyCollaborator(Email.valueOf("nullposition@y.pt"), null, CompanyIATACode.valueOf("TP"),
				Email.valueOf("contact@y.pt"), PhoneNumber.valueOf("+351910000010"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCompanyCollaboratorRejectsNullCompanyCode() {
		new CompanyCollaborator(Email.valueOf("nullcompany@y.pt"), Position.valueOf("Officer"), null,
				Email.valueOf("contact@y.pt"), PhoneNumber.valueOf("+351910000011"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureAreaCollaboratorRejectsNullAreaCode() {
		new AreaCollaborator(Email.valueOf("nullarea@y.pt"), Position.valueOf("FCO"), null,
				Email.valueOf("contact@y.pt"), PhoneNumber.valueOf("+351910000012"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCollaboratorRejectsNullContactEmail() {
		new CompanyCollaborator(Email.valueOf("nullcontact@y.pt"), Position.valueOf("Officer"),
				CompanyIATACode.valueOf("TP"), null, PhoneNumber.valueOf("+351910000013"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureCollaboratorRejectsNullContactPhoneNumber() {
		new AreaCollaborator(Email.valueOf("nullphone@y.pt"), Position.valueOf("FCO"),
				AreaCode.valueOf("LPPC"), Email.valueOf("contact@y.pt"), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureChangeContactDetailsRejectsNulls() {
		final CompanyCollaborator collaborator = new CompanyCollaborator(Email.valueOf("contactchange@y.pt"),
				Position.valueOf("Officer"), CompanyIATACode.valueOf("TP"),
				Email.valueOf("contactchange@y.pt"), PhoneNumber.valueOf("+351910000014"));

		collaborator.changeContactDetails(null, PhoneNumber.valueOf("+351910000015"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureChangeContactDetailsRejectsNullPhoneNumber() {
		final AreaCollaborator collaborator = new AreaCollaborator(Email.valueOf("contactphone@y.pt"),
				Position.valueOf("FCO"), AreaCode.valueOf("LPPC"),
				Email.valueOf("contactphone@y.pt"), PhoneNumber.valueOf("+351910000016"));

		collaborator.changeContactDetails(Email.valueOf("newcontact@y.pt"), null);
	}

	@Test
	public void ensureCollaboratorStatusValuesAreAvailable() {
		assertEquals(CollaboratorStatus.ACTIVE, CollaboratorStatus.valueOf("ACTIVE"));
		assertEquals(CollaboratorStatus.DISABLED, CollaboratorStatus.valueOf("DISABLED"));
		assertEquals(2, CollaboratorStatus.values().length);
	}

	@Test
	public void ensureOrmConstructorsAreAvailable() {
		assertNotNull(new Position());
		assertNotNull(new CompanyCollaborator());
		assertNotNull(new AreaCollaborator());
		assertNotNull(new TestCollaborator());
	}

	private static class TestCollaborator extends Collaborator {
		private static final long serialVersionUID = 1L;

		TestCollaborator() {
			super();
		}
	}
}
