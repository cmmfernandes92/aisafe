package eapli.aisafe.customercollaboratormanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.customercollaboratormanagement.domain.AreaCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Collaborator;
import eapli.aisafe.customercollaboratormanagement.domain.CollaboratorStatus;
import eapli.aisafe.customercollaboratormanagement.domain.CompanyCollaborator;
import eapli.aisafe.customercollaboratormanagement.domain.Position;
import eapli.aisafe.customercollaboratormanagement.repositories.CollaboratorRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.usermanagement.domain.PhoneNumber;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class EditCollaboratorControllerTest {

    private static final String COMPANY_CODE = "TP";

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

    private final FakeCollaboratorRepository collaboratorRepo = new FakeCollaboratorRepository();
    private final EditCollaboratorController controller = new EditCollaboratorController(collaboratorRepo);

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private CompanyCollaborator givenExistingCollaborator(final String emailStr, final String company) {
        final Email email = Email.valueOf(emailStr);
        final CompanyCollaborator c = new CompanyCollaborator(
                email, Position.valueOf("Officer"), CompanyIATACode.valueOf(company), email,
                PhoneNumber.valueOf("+351910000000"));
        collaboratorRepo.save(c);
        return c;
    }

    // -------------------------------------------------------------------------
    // activeCollaboratorsOf tests
    // -------------------------------------------------------------------------

    @Test
    public void ensureActiveCollaboratorsOfFiltersCorrectlyByCompany() {
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);
        givenExistingCollaborator("bob@test.pt", "FR"); // different company — must not appear

        final List<CompanyCollaborator> result = StreamSupport.stream(
                        controller.activeCollaboratorsOf(CompanyIATACode.valueOf(COMPANY_CODE)).spliterator(), false)
                .toList();

        assertEquals(1, result.size());
        assertEquals(Email.valueOf("alice@test.pt"), result.get(0).identity());
    }

    @Test
    public void ensureActiveCollaboratorsOfExcludesDisabledCollaborators() {
        final CompanyCollaborator c = givenExistingCollaborator("alice@test.pt", COMPANY_CODE);
        c.disable();
        collaboratorRepo.save(c);

        final List<CompanyCollaborator> result = StreamSupport.stream(
                        controller.activeCollaboratorsOf(CompanyIATACode.valueOf(COMPANY_CODE)).spliterator(), false)
                .toList();

        assertEquals(0, result.size());
    }

    // -------------------------------------------------------------------------
    // editContactInfo tests (US063)
    // -------------------------------------------------------------------------

    @Test
    public void ensureEditContactInfoUpdatesContactEmailAndPhone() {
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);

        controller.editContactInfo("alice@test.pt", "newcontact@gmail.com", "+351911111111");

        final Collaborator updated = collaboratorRepo.ofIdentity(Email.valueOf("alice@test.pt")).get();
        assertEquals(Email.valueOf("newcontact@gmail.com"), updated.contactEmail());
        assertEquals(PhoneNumber.valueOf("+351911111111"), updated.contactPhoneNumber());
    }

    @Test
    public void ensureEditContactInfoDoesNotChangeLoginIdentity() {
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);

        controller.editContactInfo("alice@test.pt", "newcontact@gmail.com", "+351911111111");

        // Login/identity email must remain unchanged (it is the @EmbeddedId)
        final Collaborator updated = collaboratorRepo.ofIdentity(Email.valueOf("alice@test.pt")).get();
        assertEquals(Email.valueOf("alice@test.pt"), updated.identity());
    }

    @Test
    public void ensureEditContactInfoDoesNotChangePosition() {
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);

        controller.editContactInfo("alice@test.pt", "newcontact@gmail.com", "+351911111111");

        final Collaborator updated = collaboratorRepo.ofIdentity(Email.valueOf("alice@test.pt")).get();
        assertEquals(Position.valueOf("Officer"), updated.position());
    }

    @Test
    public void ensureEditContactInfoDoesNotChangeStatus() {
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);

        controller.editContactInfo("alice@test.pt", "newcontact@gmail.com", "+351911111111");

        final Collaborator updated = collaboratorRepo.ofIdentity(Email.valueOf("alice@test.pt")).get();
        assertEquals(CollaboratorStatus.ACTIVE, updated.status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureEditContactInfoFailsWhenCollaboratorNotFound() {
        controller.editContactInfo("nonexistent@test.pt", "new@gmail.com", "+351911111111");
    }

    // -------------------------------------------------------------------------
    // Authorization tests
    // -------------------------------------------------------------------------

    @Test(expected = UnauthorizedException.class)
    public void ensureEditContactInfoRequiresBackofficeOperator() {
        AuthzTestHelper.loginAsAdmin();
        givenExistingCollaborator("alice@test.pt", COMPANY_CODE);
        controller.editContactInfo("alice@test.pt", "new@gmail.com", "+351911111111");
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureActiveCollaboratorsOfRequiresBackofficeOperator() {
        AuthzTestHelper.loginAsPilot();
        controller.activeCollaboratorsOf(CompanyIATACode.valueOf(COMPANY_CODE));
    }

    // -------------------------------------------------------------------------
    // Fake repository
    // -------------------------------------------------------------------------

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
        public Iterable<Collaborator> findAll() { return data.values(); }

        @Override
        public long count() { return data.size(); }

        @Override
        public void delete(final Collaborator entity) { data.remove(entity.identity()); }

        @Override
        public void deleteOfIdentity(final Email id) { data.remove(id); }

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
            return ofIdentity(email).filter(c -> c instanceof CompanyCollaborator).map(c -> (CompanyCollaborator) c);
        }

        @Override
        public Optional<AreaCollaborator> findAreaCollaboratorByEmail(final Email email) {
            return ofIdentity(email).filter(c -> c instanceof AreaCollaborator).map(c -> (AreaCollaborator) c);
        }
    }
}