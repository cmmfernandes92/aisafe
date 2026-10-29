package eapli.aisafe.customercollaboratormanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.aircontrolareamanagement.repositories.AirControlAreaRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CollaboratorStatus;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.aisafe.usermanagement.domain.SecurityClearance;
import eapli.aisafe.usermanagement.domain.SkillsAssessment;
import eapli.aisafe.usermanagement.domain.User;
import eapli.aisafe.usermanagement.domain.UserBuilderHelper;
import eapli.aisafe.usermanagement.repositories.UserRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

public class RegisterCollaboratorControllerTest {

	@BeforeClass
	public static void setUpAuthz() {
		AuthzTestHelper.configureAuthz();
	}

	@Before
	public void login() {
		AuthzTestHelper.loginAsBackofficeOperator();
	}

	@After
	public void logout() {
		AuthzTestHelper.clearSession();
	}

	private final FakeCollaboratorRepository collaboratorRepository = new FakeCollaboratorRepository();
	private final FakeUserRepository userRepository = new FakeUserRepository();
	private final FakeAirTransportCompanyRepository companyRepository = new FakeAirTransportCompanyRepository();
	private final FakeAirControlAreaRepository areaRepository = new FakeAirControlAreaRepository();
	private final RegisterCollaboratorController controller = new RegisterCollaboratorController(
			collaboratorRepository, userRepository, companyRepository, areaRepository);

	@Test
	public void ensureRegisterCompanyCollaboratorHappyPath() {
		final Email email = Email.valueOf("ana@tap.pt");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata);

		final CompanyCollaborator result = controller.registerCompanyCollaborator(
				email, Position.valueOf("Operations Manager"), iata);

		assertEquals(email, result.identity());
		assertEquals(iata, result.companyCode());
		assertEquals(Position.valueOf("Operations Manager"), result.position());
		assertEquals(email, result.contactEmail());
		assertEquals(PhoneNumber.valueOf("+351910000000"), result.contactPhoneNumber());
		assertEquals(CollaboratorStatus.ACTIVE, result.status());
		assertTrue(collaboratorRepository.ofIdentity(email).isPresent());
	}

	@Test(expected = UnauthorizedException.class)
	public void ensureRegisterCompanyCollaboratorRequiresBackofficeOperator() {
		AuthzTestHelper.loginAsAdmin();

		controller.registerCompanyCollaborator(
				Email.valueOf("ana@tap.pt"),
				Position.valueOf("Operations Manager"),
				CompanyIATACode.valueOf("TP"));
	}

	@Test
	public void ensureRegisterAreaCollaboratorHappyPath() {
		final Email email = Email.valueOf("fco@nav.pt");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		givenExistingUserWithRole(email, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		givenExistingArea(areaCode);

		final AreaCollaborator result = controller.registerAreaCollaborator(
				email, Position.valueOf("FCO"), areaCode);

		assertEquals(email, result.identity());
		assertEquals(areaCode, result.areaCode());
		assertEquals(email, result.contactEmail());
		assertEquals(PhoneNumber.valueOf("+351910000000"), result.contactPhoneNumber());
		assertEquals(CollaboratorStatus.ACTIVE, result.status());
		assertTrue(collaboratorRepository.ofIdentity(email).isPresent());
	}

	@Test(expected = UnauthorizedException.class)
	public void ensureRegisterAreaCollaboratorRequiresBackofficeOperator() {
		AuthzTestHelper.loginAsPowerUser();

		controller.registerAreaCollaborator(
				Email.valueOf("fco@nav.pt"),
				Position.valueOf("FCO"),
				AreaCode.valueOf("LPPC"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsNonExistentUserForCompanyCollaborator() {
		givenExistingCompany(CompanyIATACode.valueOf("TP"));

		controller.registerCompanyCollaborator(
				Email.valueOf("ghost@nowhere.pt"),
				Position.valueOf("Manager"),
				CompanyIATACode.valueOf("TP"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsNonExistentCompanyForCompanyCollaborator() {
		final Email email = Email.valueOf("ana@tap.pt");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);

		controller.registerCompanyCollaborator(email, Position.valueOf("Manager"), CompanyIATACode.valueOf("ZZ"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsNonExistentAreaForAreaCollaborator() {
		final Email email = Email.valueOf("fco@nav.pt");
		givenExistingUserWithRole(email, AisafeRoles.FLIGHT_CONTROL_OPERATOR);

		controller.registerAreaCollaborator(email, Position.valueOf("FCO"), AreaCode.valueOf("ZZZZ"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsUserAlreadyLinkedToAnotherCollaborator() {
		final Email email = Email.valueOf("dual@x.pt");
		final CompanyIATACode iata1 = CompanyIATACode.valueOf("TP");
		final CompanyIATACode iata2 = CompanyIATACode.valueOf("FR");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata1);
		givenExistingCompany(iata2);

		controller.registerCompanyCollaborator(email, Position.valueOf("Manager"), iata1);
		controller.registerCompanyCollaborator(email, Position.valueOf("Officer"), iata2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsUserWithoutAtccRoleForCompanyCollaborator() {
		final Email email = Email.valueOf("wrongrole@x.pt");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(email, AisafeRoles.PILOT);
		givenExistingCompany(iata);

		controller.registerCompanyCollaborator(email, Position.valueOf("Manager"), iata);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureRegisterRejectsUserWithoutFcoRoleForAreaCollaborator() {
		final Email email = Email.valueOf("wrongrole@y.pt");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		givenExistingUserWithRole(email, AisafeRoles.WEATHER_PERSON);
		givenExistingArea(areaCode);

		controller.registerAreaCollaborator(email, Position.valueOf("FCO"), areaCode);
	}

	@Test
	public void ensureRegisterAcceptsCollaboratorEmailOutsideCustomerDomain() {
		final Email email = Email.valueOf("personal@gmail.com");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata);

		final CompanyCollaborator result = controller.registerCompanyCollaborator(
				email, Position.valueOf("Manager"), iata);

		assertEquals(email, result.identity());
	}

	private void givenExistingUserWithRole(final Email email, final Role role) {
		final SystemUser systemUser = UserBuilderHelper.builder()
				.withUsername(email.toString())
				.withPassword("Password1")
				.withName("Test", "User")
				.withEmail(email.toString())
				.withRoles(role)
				.build();
		userRepository.save(new User(systemUser, email, PhoneNumber.valueOf("+351910000000"),
				SecurityClearance.valueOf(LocalDate.now().plusYears(1)),
				SkillsAssessment.valueOf(LocalDate.now().minusMonths(1))));
	}

	private void givenExistingCompany(final CompanyIATACode iataCode) {
		companyRepository.save(new AirTransportCompany(iataCode, CompanyICAOCode.valueOf(iataCode.value() + "A"),
				"Company " + iataCode.value()));
	}

	private void givenExistingArea(final AreaCode areaCode) {
		areaRepository.save(new AirControlArea(areaCode, AreaName.valueOf("Area " + areaCode.value()),
				new GeographicBoundary(new Coordinates(38.0, -10.0), new Coordinates(40.0, -8.0))));
	}

	private static class FakeCollaboratorRepository implements CollaboratorRepository {
		private final Map<Email, Collaborator> data = new HashMap<>();

		@Override
		public <S extends Collaborator> S save(final S entity) {
			data.put(entity.identity(), entity);
			return entity;
		}

		@Override
		public Optional<Collaborator> ofIdentity(final Email id) {
			return Optional.ofNullable(data.get(id));
		}

		@Override
		public Iterable<Collaborator> findAll() {
			return data.values();
		}

		@Override
		public long count() {
			return data.size();
		}

		@Override
		public void delete(final Collaborator entity) {
			data.remove(entity.identity());
		}

		@Override
		public void deleteOfIdentity(final Email id) {
			data.remove(id);
		}

		@Override
		public Iterable<CompanyCollaborator> findAllActiveByCompanyCode(final CompanyIATACode companyCode) {
			return data.values().stream()
					.filter(c -> c instanceof CompanyCollaborator)
					.map(c -> (CompanyCollaborator) c)
					.filter(c -> c.companyCode().equals(companyCode) && c.isActive())
					.toList();
		}

		@Override
		public Iterable<CompanyCollaborator> findAllByCompanyCode(final CompanyIATACode companyCode) {
			return data.values().stream()
					.filter(c -> c instanceof CompanyCollaborator)
					.map(c -> (CompanyCollaborator) c)
					.filter(c -> c.companyCode().equals(companyCode))
					.toList();
		}

		@Override
		public Iterable<AreaCollaborator> findAllActiveByAreaCode(final AreaCode areaCode) {
			return data.values().stream()
					.filter(c -> c instanceof AreaCollaborator)
					.map(c -> (AreaCollaborator) c)
					.filter(c -> c.areaCode().equals(areaCode) && c.isActive())
					.toList();
		}

		@Override
		public Iterable<AreaCollaborator> findAllByAreaCode(final AreaCode areaCode) {
			return data.values().stream()
					.filter(c -> c instanceof AreaCollaborator)
					.map(c -> (AreaCollaborator) c)
					.filter(c -> c.areaCode().equals(areaCode))
					.toList();
		}

		@Override
		public Optional<CompanyCollaborator> findCompanyCollaboratorByEmail(final Email email) {
			return ofIdentity(email).filter(c -> c instanceof CompanyCollaborator)
					.map(c -> (CompanyCollaborator) c);
		}

		@Override
		public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
			return ofIdentity(email).filter(c -> c instanceof AreaCollaborator)
					.map(c -> (AreaCollaborator) c);
		}
	}

	private static class FakeUserRepository implements UserRepository {
		private final Map<Email, User> data = new HashMap<>();

		@Override
		public <S extends User> S save(final S entity) {
			data.put(entity.identity(), entity);
			return entity;
		}

		@Override
		public Optional<User> findBySystemUser(final SystemUser systemUser) {
			return data.values().stream()
					.filter(u -> u.systemUser().equals(systemUser))
					.findFirst();
		}

		@Override
		public Optional<User> ofIdentity(final Email id) {
			return Optional.ofNullable(data.get(id));
		}

		@Override
		public Iterable<User> findAll() {
			return data.values();
		}

		@Override
		public long count() {
			return data.size();
		}

		@Override
		public void delete(final User entity) {
			data.remove(entity.identity());
		}

		@Override
		public void deleteOfIdentity(final Email id) {
			data.remove(id);
		}
	}

	private static class FakeAirTransportCompanyRepository implements AirTransportCompanyRepository {
		private final Map<CompanyIATACode, AirTransportCompany> data = new HashMap<>();

		@Override
		public <S extends AirTransportCompany> S save(final S entity) {
			data.put(entity.identity(), entity);
			return entity;
		}

		@Override
		public Optional<AirTransportCompany> findByIataCode(final CompanyIATACode iataCode) {
			return Optional.ofNullable(data.get(iataCode));
		}

		@Override
		public Optional<AirTransportCompany> findByIcaoCode(final CompanyICAOCode icaoCode) {
			return data.values().stream()
					.filter(c -> c.icaoCode().equals(icaoCode))
					.findFirst();
		}

		@Override
		public Optional<AirTransportCompany> ofIdentity(final CompanyIATACode id) {
			return findByIataCode(id);
		}

		@Override
		public Iterable<AirTransportCompany> findAll() {
			return data.values();
		}

		@Override
		public long count() {
			return data.size();
		}

		@Override
		public void delete(final AirTransportCompany entity) {
			data.remove(entity.identity());
		}

		@Override
		public void deleteOfIdentity(final CompanyIATACode id) {
			data.remove(id);
		}
	}

	private static class FakeAirControlAreaRepository implements AirControlAreaRepository {
		private final Map<AreaCode, AirControlArea> data = new HashMap<>();

		@Override
		public <S extends AirControlArea> S save(final S entity) {
			data.put(entity.identity(), entity);
			return entity;
		}

		@Override
		public Optional<AirControlArea> findByCode(final AreaCode code) {
			return Optional.ofNullable(data.get(code));
		}

		@Override
		public Optional<AirControlArea> findByName(final AreaName name) {
			return data.values().stream()
					.filter(a -> a.name().equals(name))
					.findFirst();
		}

		@Override
		public Iterable<AirControlArea> findOverlapping(final GeographicBoundary boundary) {
			return data.values().stream()
					.filter(a -> a.boundary().overlaps(boundary))
					.toList();
		}

		@Override
		public Optional<AirControlArea> ofIdentity(final AreaCode id) {
			return findByCode(id);
		}

		@Override
		public Iterable<AirControlArea> findAll() {
			return data.values();
		}

		@Override
		public long count() {
			return data.size();
		}

		@Override
		public void delete(final AirControlArea entity) {
			data.remove(entity.identity());
		}

		@Override
		public void deleteOfIdentity(final AreaCode id) {
			data.remove(id);
		}
	}
}
