package eapli.aisafe.airtransportcompanymanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.airtransportcompanymanagement.domain.AirTransportCompany;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyICAOCode;
import eapli.aisafe.airtransportcompanymanagement.repositories.AirTransportCompanyRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.domain.repositories.IntegrityViolationException;

public class RegisterAirTransportCompanyControllerTest {

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

    private final FakeAirTransportCompanyRepository repo = new FakeAirTransportCompanyRepository();
    private final RegisterAirTransportCompanyController controller =
            new RegisterAirTransportCompanyController(repo);

    @Test
    public void ensureRegisterSucceedsWithValidInput() {
        final AirTransportCompany company = controller.register("TP", "TAP", "TAP Air Portugal");
        assertNotNull(company);
        assertEquals(CompanyIATACode.valueOf("TP"), company.identity());
        assertEquals(CompanyICAOCode.valueOf("TAP"), company.icaoCode());
        assertEquals("TAP Air Portugal", company.companyName());
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterRejectsDuplicateIataCode() {
        controller.register("TP", "TAP", "TAP Air Portugal");
        controller.register("TP", "RYR", "Ryanair");
    }

    @Test(expected = IntegrityViolationException.class)
    public void ensureRegisterRejectsDuplicateIcaoCode() {
        controller.register("TP", "TAP", "TAP Air Portugal");
        controller.register("FR", "TAP", "Other Company");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsInvalidIataCode() {
        controller.register("TPX", "TAP", "TAP Air Portugal");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterRejectsBlankCompanyName() {
        controller.register("TP", "TAP", "   ");
    }

    // -------------------------------------------------------------------------
    // Fake repository
    // -------------------------------------------------------------------------

    private static class FakeAirTransportCompanyRepository implements AirTransportCompanyRepository {
        private final Map<CompanyIATACode, AirTransportCompany> data = new HashMap<>();

        @Override
        public <S extends AirTransportCompany> S save(final S entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<AirTransportCompany> ofIdentity(final CompanyIATACode id) {
            return Optional.ofNullable(data.get(id));
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

        @Override public Iterable<AirTransportCompany> findAll()          { return data.values(); }
        @Override public long count()                                      { return data.size(); }
        @Override public void delete(final AirTransportCompany e)         { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final CompanyIATACode id)  { data.remove(id); }
    }
}
