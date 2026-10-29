package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.CabinConfiguration;
import eapli.aisafe.fleetmanagement.domain.Country;
import eapli.aisafe.fleetmanagement.domain.Registration;
import eapli.aisafe.fleetmanagement.domain.SeatClass;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.physics.FlightSimulationModel;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;

/**
 * Tests for the US085 read-only {@link SimulateFlightPlanController}: it produces a
 * {@link FlightSimulationModel} for a pilot's own VALIDATED/TESTED flight and returns
 * empty for anything it must not simulate (missing args, unknown flight, another
 * pilot's flight, a plan that is not VALIDATED/TESTED). It never changes plan state.
 * Wired with fake repositories and a stub compiler — no Mockito, per convention.
 */
public class SimulateFlightPlanControllerTest {

    private static final LocalDateTime DEP = LocalDateTime.parse("2026-06-12T13:00");
    private static final LocalDateTime ARR = LocalDateTime.parse("2026-06-12T13:50");
    private static final Registration REG = Registration.valueOf("CS-TUA");
    private static final RouteName FR401 = RouteName.valueOf("FR401");
    private static final String OWNER = "pilot@aisafe.test";

    private final FakeFlightRepository flightRepository = new FakeFlightRepository();
    private final FakeAircraftRepository aircraftRepository = new FakeAircraftRepository();
    private final FakeAircraftModelRepository aircraftModelRepository = new FakeAircraftModelRepository();
    private final FakeWeatherDataRepository weatherRepository = new FakeWeatherDataRepository();

    private final FlightPlanCompiler compiler = raw -> FlightPlanCompilationResult.ok(fr401Plan());

    private SimulateFlightPlanController controller() {
        return new SimulateFlightPlanController(flightRepository, compiler,
                weatherRepository, aircraftRepository, aircraftModelRepository);
    }

    /** A VALIDATED FR401 flight owned by OWNER, with its aircraft/model registered. */
    private Flight givenValidatedFlight() {
        final long modelId = aircraftModelRepository.saveAndGetId(a320neo());
        aircraftRepository.save(new Aircraft(REG, modelId, CompanyIATACode.valueOf("FR"),
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)), new Country("Portugal"), 0));
        final Flight flight = new Flight(FR401, Email.valueOf(OWNER), REG, DEP);
        flight.importDsl("flight FR401 { type: regular ... }", OWNER); // DRAFT -> VALIDATED
        flightRepository.save(flight);
        return flight;
    }

    @Test
    public void ensureOwnedValidatedFlightIsSimulated() {
        final Flight flight = givenValidatedFlight();

        final Optional<FlightSimulationModel> model =
                controller().simulateFor(OWNER, flight.identity());

        assertTrue(model.isPresent());
        assertTrue(model.get().passed());
        assertEquals("A320neo", model.get().aircraft().modelName());
    }

    @Test
    public void ensureTestedFlightIsStillSimulated() {
        final Flight flight = givenValidatedFlight();
        // promote VALIDATED -> TESTED through the same production service
        flight.testCurrentPlan(new SimulatorV1FlightPlanTestService(
                compiler, weatherRepository, aircraftRepository, aircraftModelRepository));

        final Optional<FlightSimulationModel> model =
                controller().simulateFor(OWNER, flight.identity());

        assertTrue(model.isPresent());
    }

    @Test
    public void ensureNullArgumentsReturnEmpty() {
        final Flight flight = givenValidatedFlight();
        assertFalse(controller().simulateFor(null, flight.identity()).isPresent());
        assertFalse(controller().simulateFor(OWNER, null).isPresent());
    }

    @Test
    public void ensureUnknownFlightReturnsEmpty() {
        final FlightId unknown = new FlightId(FR401, DEP.toLocalDate());
        assertFalse(controller().simulateFor(OWNER, unknown).isPresent());
    }

    @Test
    public void ensureAnotherPilotsFlightReturnsEmpty() {
        final Flight flight = givenValidatedFlight();
        assertFalse(controller().simulateFor("intruder@aisafe.test", flight.identity()).isPresent());
    }

    @Test
    public void ensureDraftPlanIsNotSimulated() {
        // a freshly created flight has a DRAFT plan (no DSL imported yet)
        final Flight draft = new Flight(FR401, Email.valueOf(OWNER), REG, DEP);
        flightRepository.save(draft);

        assertFalse(controller().simulateFor(OWNER, draft.identity()).isPresent());
    }

    // ---- fixtures ----

    private static AircraftModel a320neo() {
        final Maker airbus = Maker.valueOf("Airbus");
        final EngineModel leap = new EngineModel(EngineModelName.valueOf("CFM LEAP-1A"), airbus,
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), airbus, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833,
                122.6, 35.8, 0.024, 1.5, 6300, Set.of(leap));
    }

    private static ParsedFlightPlan fr401Plan() {
        return new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR, 3300, ParsedFlightPlan.FuelUnit.KG,
                List.of(new ParsedSegment(1, 0, 41.2356, -8.6781, 40.0100, -8.9000, 9000, 350, 8.0),
                        new ParsedSegment(1, 1, 40.0100, -8.9000, 38.7742, -9.1342, 9000, 340, 6.5)));
    }

    // ---- fake repositories ----

    private static final class FakeFlightRepository
            implements eapli.aisafe.flightmanagement.repositories.FlightRepository {

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }
        private final Map<FlightId, Flight> data = new HashMap<>();

        @Override public <S extends Flight> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<Flight> ofIdentity(final FlightId id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean containsOfIdentity(final FlightId id) { return data.containsKey(id); }
        @Override public Iterable<Flight> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final Flight e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final FlightId id) { data.remove(id); }
        @Override public boolean existsForRouteOnDate(final RouteName route, final LocalDate date) {
            return data.values().stream().anyMatch(f -> f.route().equals(route)
                    && f.departure().toLocalDate().equals(date));
        }
        @Override public Iterable<Flight> flightsOf(final Email pilot) {
            return data.values().stream().filter(f -> f.pilot().equals(pilot)).toList();
        }
    }

    private static final class FakeAircraftRepository implements AircraftRepository {
        private final Map<Registration, Aircraft> data = new HashMap<>();

        @Override public <S extends Aircraft> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<Aircraft> ofIdentity(final Registration id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean containsOfIdentity(final Registration id) { return data.containsKey(id); }
        @Override public Iterable<Aircraft> findAllByCompany(final CompanyIATACode c) {
            return data.values().stream().filter(a -> a.companyCode().equals(c)).toList();
        }
        @Override public Iterable<Aircraft> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final Aircraft e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final Registration id) { data.remove(id); }
    }

    private static final class FakeAircraftModelRepository implements AircraftModelRepository {
        private final Map<Long, AircraftModel> data = new HashMap<>();
        private long nextId = 1L;

        long saveAndGetId(final AircraftModel e) { final long id = nextId++; data.put(id, e); return id; }
        @Override public <S extends AircraftModel> S save(final S e) { data.put(nextId++, e); return e; }
        @Override public Optional<AircraftModel> ofIdentity(final Long id) { return Optional.ofNullable(data.get(id)); }
        @Override public Optional<AircraftModel> findByNameAndMaker(final AircraftModelName n, final Maker m) {
            return data.values().stream().filter(x -> x.modelName().equals(n) && x.maker().equals(m)).findFirst();
        }
        @Override public boolean existsByNameAndMaker(final AircraftModelName n, final Maker m) {
            return findByNameAndMaker(n, m).isPresent();
        }
        @Override public Iterable<AircraftModel> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final AircraftModel e) { data.values().removeIf(e::sameAs); }
        @Override public void deleteOfIdentity(final Long id) { data.remove(id); }
    }

    private static final class FakeWeatherDataRepository implements WeatherDataRepository {
        private final Map<WeatherDataId, WeatherData> data = new HashMap<>();

        @Override public <S extends WeatherData> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<WeatherData> ofIdentity(final WeatherDataId id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean containsOfIdentity(final WeatherDataId id) { return data.containsKey(id); }
        @Override public Iterable<WeatherData> findByAreaCodeAndDay(
                final eapli.aisafe.aircontrolareamanagement.domain.AreaCode a, final LocalDate d) {
            return data.values().stream().filter(w -> w.areaCode().equals(a)).toList();
        }
        @Override public Iterable<WeatherData> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final WeatherData e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final WeatherDataId id) { data.remove(id); }
    }
}