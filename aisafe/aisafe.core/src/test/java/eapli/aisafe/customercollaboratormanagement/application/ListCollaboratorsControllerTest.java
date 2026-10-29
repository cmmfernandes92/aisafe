package eapli.aisafe.customercollaboratormanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

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

public class ListCollaboratorsControllerTest {

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
	private final RegisterCollaboratorController registerController = new RegisterCollaboratorController(
			collaboratorRepository, userRepository, companyRepository, areaRepository);
	private final ListCollaboratorsController listController = new ListCollaboratorsController(
			collaboratorRepository, userRepository, companyRepository, areaRepository);

	@Test
	public void ensureProductionConstructorCanBeUsed() {
		assertNotNull(new ListCollaboratorsController(
				collaboratorRepository, userRepository, companyRepository, areaRepository));
	}

	@Test
	public void ensureActiveCompanyCollaboratorsAreListed() {
		final Email email = Email.valueOf("ana@tap.pt");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata);
		registerController.registerCompanyCollaborator(email, Position.valueOf("Operations Manager"), iata);

		final Iterable<CompanyCollaborator> result = listController.activeCollaboratorsOf(iata);

		final List<CompanyCollaborator> list = StreamSupport.stream(result.spliterator(), false).toList();
		assertEquals(1, list.size());
		assertEquals(email, list.get(0).identity());
		assertEquals(CollaboratorStatus.ACTIVE, list.get(0).status());
	}

	@Test
	public void ensureActiveAreaCollaboratorsAreListed() {
		final Email email = Email.valueOf("fco@nav.pt");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		givenExistingUserWithRole(email, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		givenExistingArea(areaCode);
		registerController.registerAreaCollaborator(email, Position.valueOf("FCO"), areaCode);

		final Iterable<AreaCollaborator> result = listController.activeCollaboratorsOf(areaCode);

		final List<AreaCollaborator> list = StreamSupport.stream(result.spliterator(), false).toList();
		assertEquals(1, list.size());
		assertEquals(email, list.get(0).identity());
		assertEquals(CollaboratorStatus.ACTIVE, list.get(0).status());
	}

	@Test
	public void ensureDisabledCompanyCollaboratorsAreExcluded() {
		final Email activeEmail = Email.valueOf("active@tap.pt");
		final Email disabledEmail = Email.valueOf("disabled@tap.pt");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(activeEmail, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingUserWithRole(disabledEmail, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata);
		registerController.registerCompanyCollaborator(activeEmail, Position.valueOf("Manager"), iata);
		final CompanyCollaborator toDisable = registerController.registerCompanyCollaborator(
				disabledEmail, Position.valueOf("Officer"), iata);
		toDisable.disable();
		collaboratorRepository.save(toDisable);

		final Iterable<CompanyCollaborator> result = listController.activeCollaboratorsOf(iata);

		final List<CompanyCollaborator> list = StreamSupport.stream(result.spliterator(), false).toList();
		assertEquals(1, list.size());
		assertEquals(activeEmail, list.get(0).identity());
	}

	@Test
	public void ensureDisabledAreaCollaboratorsAreExcluded() {
		final Email activeEmail = Email.valueOf("active@nav.pt");
		final Email disabledEmail = Email.valueOf("disabled@nav.pt");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		givenExistingUserWithRole(activeEmail, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		givenExistingUserWithRole(disabledEmail, AisafeRoles.FLIGHT_CONTROL_OPERATOR);
		givenExistingArea(areaCode);
		registerController.registerAreaCollaborator(activeEmail, Position.valueOf("FCO"), areaCode);
		final AreaCollaborator toDisable = registerController.registerAreaCollaborator(
				disabledEmail, Position.valueOf("FCO"), areaCode);
		toDisable.disable();
		collaboratorRepository.save(toDisable);

		final Iterable<AreaCollaborator> result = listController.activeCollaboratorsOf(areaCode);

		final List<AreaCollaborator> list = StreamSupport.stream(result.spliterator(), false).toList();
		assertEquals(1, list.size());
		assertEquals(activeEmail, list.get(0).identity());
	}

	@Test
	public void ensureEmptyResultIsValidForCompanyWithNoActiveCollaborators() {
		final CompanyIATACode iata = CompanyIATACode.valueOf("FR");
		givenExistingCompany(iata);

		final Iterable<CompanyCollaborator> result = listController.activeCollaboratorsOf(iata);

		assertFalse(result.iterator().hasNext());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureListRejectsNonExistentCompany() {
		listController.activeCollaboratorsOf(CompanyIATACode.valueOf("ZZ"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ensureListRejectsNonExistentArea() {
		listController.activeCollaboratorsOf(AreaCode.valueOf("ZZZZ"));
	}

	@Test
	public void ensureCollaboratorNameIsResolvedFromLinkedUser() {
		final Email email = Email.valueOf("ana@tap.pt");
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		givenExistingUserWithRole(email, AisafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
		givenExistingCompany(iata);
		registerController.registerCompanyCollaborator(email, Position.valueOf("Operations Manager"), iata);

		final Optional<User> resolved = listController.userOf(email);

		assertTrue(resolved.isPresent());
		assertEquals("Test User", resolved.get().systemUser().name().toString());
	}

	@Test
	public void ensureAllCustomersCanBeListedForSelection() {
		final CompanyIATACode iata = CompanyIATACode.valueOf("TP");
		final AreaCode areaCode = AreaCode.valueOf("LPPC");
		givenExistingCompany(iata);
		givenExistingArea(areaCode);

		final List<AirTransportCompany> companies = StreamSupport
				.stream(listController.allCompanies().spliterator(), false).toList();
		final List<AirControlArea> areas = StreamSupport
				.stream(listController.allAreas().spliterator(), false).toList();

		assertEquals(1, companies.size());
		assertEquals(iata, companies.get(0).identity());
		assertEquals(1, areas.size());
		assertEquals(areaCode, areas.get(0).identity());
	}

	@Test
	public void ensurePublicQueriesRequireAuthorization() {
		AuthzTestHelper.loginAsAdmin();

		assertRequiresAuthorization(() -> listController.allCompanies());
		assertRequiresAuthorization(() -> listController.allAreas());
		assertRequiresAuthorization(() -> listController.activeCollaboratorsOf(CompanyIATACode.valueOf("TP")));
		assertRequiresAuthorization(() -> listController.activeCollaboratorsOf(AreaCode.valueOf("LPPC")));
		assertRequiresAuthorization(() -> listController.userOf(Email.valueOf("user@x.pt")));
	}

	private void assertRequiresAuthorization(final Runnable query) {
		try {
			query.run();
		} catch (final UnauthorizedException e) {
			return;
		}
		throw new AssertionError("Expected authorization to be enforced");
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
