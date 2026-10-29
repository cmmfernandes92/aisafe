package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanStatus;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.aisafe.test.AuthzTestHelper;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.weather.domain.AltitudeBand;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.domain.WeatherWindDirection;
import eapli.aisafe.weather.domain.WeatherWindSpeed;
import eapli.aisafe.weather.domain.WeatherZone;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;

/**
 * Controller-level coverage for US082. The controller has no constructor
 * injection, so the test installs a fake {@link RepositoryFactory} into
 * {@link PersistenceContext} before constructing it. That exercises the
 * production controller without requiring production-code changes.
 */
public class AttachWeatherToPlanControllerTest {

    private static final RouteName TP123 = RouteName.valueOf("TP123");
    private static final LocalDate MAY_20 = LocalDate.of(2026, 5, 20);
    private static final SegmentKey FIRST_SEGMENT = SegmentKey.of(1, 0);
    private static final Email PILOT_EMAIL = Email.valueOf(AuthzTestHelper.PILOT + "@aisafe.test");
    private static final Email OTHER_PILOT_EMAIL = Email.valueOf("other.pilot@aisafe.test");

    private FakeFlightRepository flightRepository;
    private FakeWeatherDataRepository weatherRepository;
    private RepositoryFactory previousFactory;
    private AttachWeatherToPlanController controller;

    @BeforeClass
    public static void setUpAuthz() {
        AuthzTestHelper.configureAuthz();
    }

    @Before
    public void setUp() throws Exception {
        this.flightRepository = new FakeFlightRepository();
        this.weatherRepository = new FakeWeatherDataRepository();
        this.previousFactory = installRepositoryFactory(this.flightRepository, this.weatherRepository);
        AuthzTestHelper.loginAsPilot();
        this.controller = new AttachWeatherToPlanController();
    }

    @After
    public void tearDown() throws Exception {
        AuthzTestHelper.clearSession();
        restoreRepositoryFactory(this.previousFactory);
    }

    @Test
    public void ensureAttachWeatherPersistsAssignmentForOwnFlight() {
        final Flight flight = givenValidatedFlightOwnedBy(PILOT_EMAIL);
        final WeatherData forecast = givenWeatherData();
        flightRepository.resetSaveCount();

        controller.attachWeather(flight.identity(), Map.of(FIRST_SEGMENT, forecast.identity()));

        final Flight saved = flightRepository.ofIdentity(flight.identity()).orElseThrow();
        assertEquals(forecast.identity(), saved.currentPlan().weatherAssignmentsBySegment().get(FIRST_SEGMENT));
        assertEquals("updated aggregate should be saved once", 1, flightRepository.saveCount());
    }

    @Test
    public void ensureAttachWeatherDemotesTestedPlanToValidated() {
        final Flight flight = givenValidatedFlightOwnedBy(PILOT_EMAIL);
        flight.testCurrentPlan((f, p) -> FlightPlanTestResult.success("ok"));
        assertEquals(FlightPlanStatus.TESTED, flight.currentPlan().status());
        final WeatherData forecast = givenWeatherData();

        controller.attachWeather(flight.identity(), Map.of(FIRST_SEGMENT, forecast.identity()));

        assertEquals(FlightPlanStatus.VALIDATED,
                flightRepository.ofIdentity(flight.identity()).orElseThrow().currentPlan().status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAttachWeatherRejectsUnknownWeatherData() {
        final Flight flight = givenValidatedFlightOwnedBy(PILOT_EMAIL);

        controller.attachWeather(flight.identity(), Map.of(FIRST_SEGMENT, WeatherDataId.newId()));
    }

    @Test(expected = IllegalStateException.class)
    public void ensureAttachWeatherRejectsFlightOwnedByAnotherPilot() {
        final Flight flight = givenValidatedFlightOwnedBy(OTHER_PILOT_EMAIL);
        final WeatherData forecast = givenWeatherData();

        controller.attachWeather(flight.identity(), Map.of(FIRST_SEGMENT, forecast.identity()));
    }

    @Test(expected = UnauthorizedException.class)
    public void ensureAttachWeatherRequiresPilotRole() {
        final Flight flight = givenValidatedFlightOwnedBy(PILOT_EMAIL);
        final WeatherData forecast = givenWeatherData();
        AuthzTestHelper.loginAsAdmin();

        controller.attachWeather(flight.identity(), Map.of(FIRST_SEGMENT, forecast.identity()));
    }

    private Flight givenValidatedFlightOwnedBy(final Email owner) {
        final Flight flight = new Flight(TP123, owner, Registration.valueOf("CS-TUA"),
                MAY_20.atTime(8, 30));
        flight.importDsl("flight TP123 {\n  type: regular\n  ...\n}", owner.toString());
        return flightRepository.save(flight);
    }

    private WeatherData givenWeatherData() {
        return weatherRepository.save(new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(LocalDateTime.of(2026, 5, 20, 8, 0),
                        LocalDateTime.of(2026, 5, 20, 12, 0)),
                new WeatherZone(new Coordinates(38.0, -10.0), new Coordinates(42.0, -8.0),
                        new AltitudeBand(0.0, 40000.0)),
                new WeatherWindSpeed(12.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA")));
    }

    private static RepositoryFactory installRepositoryFactory(final FlightRepository flights,
                                                              final WeatherDataRepository weatherData) throws Exception {
        final Field factoryField = PersistenceContext.class.getDeclaredField("theFactory");
        factoryField.setAccessible(true);
        final RepositoryFactory previous = (RepositoryFactory) factoryField.get(null);
        factoryField.set(null, fakeRepositoryFactory(flights, weatherData));
        return previous;
    }

    private static void restoreRepositoryFactory(final RepositoryFactory previous) throws Exception {
        final Field factoryField = PersistenceContext.class.getDeclaredField("theFactory");
        factoryField.setAccessible(true);
        factoryField.set(null, previous);
    }

    private static RepositoryFactory fakeRepositoryFactory(final FlightRepository flights,
                                                           final WeatherDataRepository weatherData) {
        return (RepositoryFactory) Proxy.newProxyInstance(
                RepositoryFactory.class.getClassLoader(),
                new Class<?>[] { RepositoryFactory.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "flights":
                            return flights;
                        case "weatherData":
                            return weatherData;
                        case "newTransactionalContext":
                            return null;
                        case "toString":
                            return "FakeRepositoryFactory";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static final class FakeFlightRepository implements FlightRepository {

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }
        private final Map<FlightId, Flight> data = new HashMap<>();
        private int saves;

        int saveCount() {
            return this.saves;
        }

        void resetSaveCount() {
            this.saves = 0;
        }

        @Override
        public <S extends Flight> S save(final S entity) {
            this.data.put(entity.identity(), entity);
            this.saves++;
            return entity;
        }

        @Override
        public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return this.data.containsKey(new FlightId(route, date));
        }

        @Override
        public Iterable<Flight> flightsOf(final Email pilot) {
            return this.data.values().stream().filter(f -> f.pilot().equals(pilot)).toList();
        }

        @Override
        public Optional<Flight> ofIdentity(final FlightId id) {
            return Optional.ofNullable(this.data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final FlightId id) {
            return this.data.containsKey(id);
        }

        @Override
        public Iterable<Flight> findAll() {
            return this.data.values();
        }

        @Override
        public long count() {
            return this.data.size();
        }

        @Override
        public void delete(final Flight entity) {
            this.data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final FlightId id) {
            this.data.remove(id);
        }
    }

    private static final class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

        @Override
        public <S extends WeatherData> S save(final S entity) {
            this.data.put(entity.identity(), entity);
            return entity;
        }

        @Override
        public Optional<WeatherData> ofIdentity(final WeatherDataId id) {
            return Optional.ofNullable(this.data.get(id));
        }

        @Override
        public boolean containsOfIdentity(final WeatherDataId id) {
            return this.data.containsKey(id);
        }

        @Override
        public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode areaCode, final LocalDate day) {
            return this.data.values().stream()
                    .filter(w -> w.areaCode().equals(areaCode)
                            && w.timePeriod().startTime().toLocalDate().equals(day))
                    .toList();
        }

        @Override
        public Iterable<WeatherData> findAll() {
            return this.data.values();
        }

        @Override
        public long count() {
            return this.data.size();
        }

        @Override
        public void delete(final WeatherData entity) {
            this.data.remove(entity.identity());
        }

        @Override
        public void deleteOfIdentity(final WeatherDataId id) {
            this.data.remove(id);
        }
    }
}
