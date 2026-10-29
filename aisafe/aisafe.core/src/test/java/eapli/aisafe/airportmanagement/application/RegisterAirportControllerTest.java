package eapli.aisafe.airportmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import eapli.aisafe.airportmanagement.domain.Airport;
import eapli.aisafe.airportmanagement.domain.AirportIATACode;
import eapli.aisafe.airportmanagement.domain.AirportICAOCode;
import eapli.aisafe.airportmanagement.repositories.AirportRepository;
import eapli.aisafe.countrymanagement.domain.Country;
import eapli.aisafe.countrymanagement.domain.CountryCode;
import eapli.aisafe.countrymanagement.domain.CountryName;
import eapli.aisafe.countrymanagement.repositories.CountryRepository;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

public class RegisterAirportControllerTest {

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

    private static class FakeAirportRepository implements AirportRepository {
        private final Map<AirportICAOCode, Airport> data = new HashMap<>();

        @Override
        public Airport save(final Airport entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
            return Optional.ofNullable(data.get(icaoCode));
        }

        @Override
        public Optional<Airport> findByIataCode(final AirportIATACode iataCode) {
            return data.values().stream()
                    .filter(a -> a.iataCode().equals(iataCode))
                    .findFirst();
        }

        @Override
        public Iterable<Airport> findByAreaCode(final AreaCode areaCode) {
            return data.values().stream()
                    .filter(a -> a.areaCode().equals(areaCode))
                    .toList();
        }

        @Override
        public Iterable<Airport> findByCountryCode(final CountryCode countryCode) {
            return data.values().stream()
                    .filter(a -> a.countryCode().equals(countryCode))
                    .toList();
        }

        @Override
        public Optional<Airport> ofIdentity(final AirportICAOCode id) {
            return findByIcaoCode(id);
        }

        @Override
        public Iterable<Airport> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Airport entity) {
            throw new UnsupportedOperationException("not used in tests");
        }

        @Override
        public void deleteOfIdentity(final AirportICAOCode id) {
            throw new UnsupportedOperationException("not used in tests");
        }
    }

    private static class FakeAirControlAreaRepository implements AirControlAreaRepository {
        private final Map<AreaCode, AirControlArea> data = new HashMap<>();

        @Override
        public AirControlArea save(final AirControlArea entity) {
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
            throw new UnsupportedOperationException("not used in tests");
        }

        @Override
        public void deleteOfIdentity(final AreaCode id) {
            throw new UnsupportedOperationException("not used in tests");
        }
    }

    private static class FakeCountryRepository implements CountryRepository {
        private final Map<CountryCode, Country> data = new HashMap<>();

        @Override
        public Country save(final Country entity) {
            data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<Country> findByCode(final CountryCode code) {
            return Optional.ofNullable(data.get(code));
        }

        @Override
        public Optional<Country> findByName(final CountryName name) {
            return data.values().stream()
                    .filter(c -> c.name().equals(name))
                    .findFirst();
        }

        @Override
        public Optional<Country> ofIdentity(final CountryCode id) {
            return findByCode(id);
        }

        @Override
        public Iterable<Country> findAll() {
            return data.values();
        }

        @Override
        public long count() {
            return data.size();
        }

        @Override
        public void delete(final Country entity) {
            throw new UnsupportedOperationException("not used in tests");
        }

        @Override
        public void deleteOfIdentity(final CountryCode id) {
            throw new UnsupportedOperationException("not used in tests");
        }
    }

    private RegisterAirportController newControllerWithReferenceData() {
        final FakeAirportRepository airportRepository = new FakeAirportRepository();
        final FakeAirControlAreaRepository airControlAreaRepository = new FakeAirControlAreaRepository();
        final FakeCountryRepository countryRepository = new FakeCountryRepository();

        airControlAreaRepository.save(new AirControlArea(new AreaCode("LPPC"), new AreaName("Lisbon TMA"),
                new GeographicBoundary(new Coordinates(38.0, -10.0), new Coordinates(40.0, -8.0))));
        countryRepository.save(Country.valueOf("351", "Portugal"));

        return new RegisterAirportController(airportRepository, airControlAreaRepository, countryRepository);
    }

    @Test
    public void ensureRegisterAirportSucceedsWithValidInput() {
        final RegisterAirportController controller = newControllerWithReferenceData();

        final Airport airport = controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");

        assertNotNull(airport);
        assertEquals("LPPT", airport.identity().value());
        assertEquals("LIS", airport.iataCode().value());
    }

    @Test
    public void ensurePowerUserCanRegisterAirport() {
        AuthzTestHelper.loginAsPowerUser();
        final RegisterAirportController controller = newControllerWithReferenceData();

        final Airport airport = controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");

        assertNotNull(airport);
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureRegisterAirportRequiresBackofficeOperatorOrPowerUser() {
        AuthzTestHelper.loginAsPilot();
        final RegisterAirportController controller = newControllerWithReferenceData();

        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsDuplicateICAOCode() {
        final RegisterAirportController controller = newControllerWithReferenceData();
        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");
        controller.registerAirport("LPPT", "OPO", "Other Airport",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsDuplicateIATACode() {
        final RegisterAirportController controller = newControllerWithReferenceData();
        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");
        controller.registerAirport("LPPP", "LIS", "Other Airport",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsUnknownCountry() {
        final RegisterAirportController controller = newControllerWithReferenceData();

        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "34", "LPPC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsUnknownArea() {
        final RegisterAirportController controller = newControllerWithReferenceData();

        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 38.7742, -9.1342, "351", "LPPO");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsCoordinatesOutsideArea() {
        final RegisterAirportController controller = newControllerWithReferenceData();

        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 41.2356, -8.6781, "351", "LPPC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureRegisterAirportRejectsInvalidCoordinates() {
        final RegisterAirportController controller = newControllerWithReferenceData();

        controller.registerAirport("LPPT", "LIS", "Humberto Delgado",
                "Lisbon", 114.0, 91.0, -9.1342, "351", "LPPC");
    }
}
