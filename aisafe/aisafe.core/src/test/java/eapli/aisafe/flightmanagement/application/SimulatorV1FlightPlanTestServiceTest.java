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

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
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
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanIssue;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.physics.FlightSimulationModel;
import eapli.aisafe.flightmanagement.domain.physics.WindSource;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.aisafe.flightroutemanagement.domain.RouteName;

/**
 * End-to-end tests for the US085 {@link SimulatorV1FlightPlanTestService} (the
 * production {@link FlightPlanTestService}): compiler port + weather resolution +
 * §3.3 physics + verdict, wired with fake repositories and a stub compiler — no
 * Mockito, matching the codebase convention.
 */
public class SimulatorV1FlightPlanTestServiceTest {

    private static final LocalDateTime DEP = LocalDateTime.parse("2026-06-12T13:00");
    private static final LocalDateTime ARR = LocalDateTime.parse("2026-06-12T13:50");
    private static final Registration REG = Registration.valueOf("CS-TUA");
    private static final RouteName FR401 = RouteName.valueOf("FR401");

    private final FakeAircraftRepository aircraftRepository = new FakeAircraftRepository();
    private final FakeAircraftModelRepository aircraftModelRepository = new FakeAircraftModelRepository();
    private final FakeWeatherDataRepository weatherRepository = new FakeWeatherDataRepository();

    private Flight givenFr401Flight() {
        final long modelId = aircraftModelRepository.saveAndGetId(a320neo());
        aircraftRepository.save(new Aircraft(REG, modelId, CompanyIATACode.valueOf("FR"),
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)), new Country("Portugal"), 0));
        final Flight flight = new Flight(FR401, Email.valueOf("pilot@aisafe.test"), REG, DEP);
        flight.importDsl("flight FR401 { type: regular ... }", "pilot@aisafe.test");
        return flight;
    }

    private SimulatorV1FlightPlanTestService serviceCompiling(final ParsedFlightPlan plan) {
        return new SimulatorV1FlightPlanTestService(
                raw -> FlightPlanCompilationResult.ok(plan),
                weatherRepository, aircraftRepository, aircraftModelRepository);
    }

    @Test
    public void ensureValidFr401PlanPasses() {
        final Flight flight = givenFr401Flight();
        final FlightPlanTestResult result =
                serviceCompiling(fr401Plan(3300, 9000, DEP, ARR)).test(flight, flight.currentPlan());

        assertTrue("FR401 should pass: " + result.message(), result.isSuccess());
    }

    @Test
    public void ensureSimulateProducesResolvedSegmentsAndPhysics() {
        final Flight flight = givenFr401Flight();
        final FlightSimulationModel model =
                serviceCompiling(fr401Plan(3300, 9000, DEP, ARR)).simulate(flight, flight.currentPlan());

        assertTrue(model.passed());
        assertEquals(2, model.segments().size());
        assertEquals(WindSource.DSL_BASELINE, model.segments().get(0).windSource()); // nothing attached
        assertEquals("A320neo", model.aircraft().modelName());
        assertEquals(10.45, model.aircraft().aspectRatio(), 0.05);  // from wingSpan 35.8 / area 122.6
        assertTrue(model.tripBurnKg() > 0);
        assertTrue("loaded must cover required", model.loadedFuelKg() >= model.requiredFuelKg());
    }

    @Test
    public void ensureInsufficientFuelFails() {
        final Flight flight = givenFr401Flight();
        final FlightPlanTestResult result =
                serviceCompiling(fr401Plan(800, 9000, DEP, ARR)).test(flight, flight.currentPlan());

        assertFalse(result.isSuccess());
        assertTrue(result.message(), result.message().contains("Insufficient fuel"));
    }

    @Test
    public void ensureAltitudeAboveServiceCeilingFails() {
        final Flight flight = givenFr401Flight();
        final FlightPlanTestResult result =
                serviceCompiling(fr401Plan(3300, 13000, DEP, ARR)).test(flight, flight.currentPlan());

        assertFalse(result.isSuccess());
        assertTrue(result.message(), result.message().contains("exceeds service ceiling"));
    }

    @Test
    public void ensureDepartureMismatchFails() {
        final Flight flight = givenFr401Flight(); // entity departs 13:00
        final LocalDateTime wrongDep = LocalDateTime.parse("2026-06-12T15:00");
        final FlightPlanTestResult result =
                serviceCompiling(fr401Plan(3300, 9000, wrongDep, ARR)).test(flight, flight.currentPlan());

        assertFalse(result.isSuccess());
        assertTrue(result.message(), result.message().contains("Departure mismatch"));
    }

    @Test
    public void ensureDslCompileErrorsAreReportedWithTypeAndLocation() {
        final Flight flight = givenFr401Flight();
        final FlightPlanCompiler failing = raw -> FlightPlanCompilationResult.failed(
                List.of(new FlightPlanIssue("SEMANTIC", "SEM01", "wind speed must be positive", 27, 14)));
        final SimulatorV1FlightPlanTestService service = new SimulatorV1FlightPlanTestService(
                failing, weatherRepository, aircraftRepository, aircraftModelRepository);

        final FlightPlanTestResult result = service.test(flight, flight.currentPlan());

        assertFalse(result.isSuccess());
        assertTrue(result.message(), result.message().contains("SEMANTIC"));
        assertTrue(result.message(), result.message().contains("27:14"));
    }

    @Test
    public void ensureAttachedForecastOverridesBaselineWindWithKnotsConversion() {
        final Flight flight = givenFr401Flight();
        // attach a 20 kt / 90° forecast to leg 1, segment 0
        final WeatherData forecast = weatherRepository.saveWithWind(20.0, 90.0);
        final Map<eapli.aisafe.flightmanagement.domain.SegmentKey, WeatherDataId> attach = new HashMap<>();
        attach.put(eapli.aisafe.flightmanagement.domain.SegmentKey.of(1, 0), forecast.identity());
        flight.attachWeatherToCurrentPlan(attach);

        final FlightSimulationModel model =
                serviceCompiling(fr401Plan(3300, 9000, DEP, ARR)).simulate(flight, flight.currentPlan());

        assertEquals(WindSource.ATTACHED_FORECAST, model.segments().get(0).windSource());
        assertEquals(90.0, model.segments().get(0).windDirectionDeg(), 1e-9);
        assertEquals(20.0 * 0.514444, model.segments().get(0).windSpeedMps(), 1e-6); // knots → m/s
        assertEquals(WindSource.DSL_BASELINE, model.segments().get(1).windSource()); // seg 1 untouched
    }

    @Test
    public void ensureFuelInLitresIsConvertedToKilograms() {
        final Flight flight = givenFr401Flight();
        final ParsedFlightPlan litres = new ParsedFlightPlan("FR401", "LPPR", "LPPT", DEP, ARR,
                5000.0, ParsedFlightPlan.FuelUnit.L,
                List.of(new ParsedSegment(1, 0, 41.2356, -8.6781, 40.0100, -8.9000, 3300, 350, 8.0),
                        new ParsedSegment(1, 1, 40.0100, -8.9000, 38.7742, -9.1342, 3300, 340, 6.5)));

        final FlightSimulationModel model = serviceCompiling(litres).simulate(flight, flight.currentPlan());

        // 5000 L of JET A-1 at 0.804 kg/L = 4020 kg loaded (litre→kg owned by the physics layer)
        assertEquals(5000.0 * SimulatorV1FlightPlanTestService.FUEL_DENSITY_KG_PER_L,
                model.loadedFuelKg(), 1e-9);
    }

    @Test
    public void ensureCompileFailureWithoutIssuesYieldsGenericMessage() {
        final Flight flight = givenFr401Flight();
        final FlightPlanCompiler emptyFailure = raw -> FlightPlanCompilationResult.failed(List.of());
        final SimulatorV1FlightPlanTestService service = new SimulatorV1FlightPlanTestService(
                emptyFailure, weatherRepository, aircraftRepository, aircraftModelRepository);

        final FlightPlanTestResult result = service.test(flight, flight.currentPlan());

        assertFalse(result.isSuccess());
        assertTrue(result.message(), result.message().contains("could not be compiled"));
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

    private static ParsedFlightPlan fr401Plan(final double fuelKg, final double altM,
            final LocalDateTime dep, final LocalDateTime arr) {
        return new ParsedFlightPlan("FR401", "LPPR", "LPPT", dep, arr, fuelKg, ParsedFlightPlan.FuelUnit.KG,
                List.of(
                        new ParsedSegment(1, 0, 41.2356, -8.6781, 40.0100, -8.9000, altM, 350, 8.0),
                        new ParsedSegment(1, 1, 40.0100, -8.9000, 38.7742, -9.1342, altM, 340, 6.5)));
    }

    // ---- fake repositories ----

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

        /** Saves a wind-only forecast (speed in knots, direction in degrees) and returns it. */
        WeatherData saveWithWind(final double knots, final double dirDeg) {
            final WeatherData wd = new WeatherData(
                    AreaCode.valueOf("LPPO"),
                    new eapli.aisafe.weather.domain.TimePeriod(DEP.minusHours(1), DEP.plusHours(2)),
                    new eapli.aisafe.weather.domain.WeatherZone(
                            new eapli.aisafe.aircontrolareamanagement.domain.Coordinates(40.0, -10.0),
                            new eapli.aisafe.aircontrolareamanagement.domain.Coordinates(42.0, -8.0),
                            new eapli.aisafe.weather.domain.AltitudeBand(0.0, 40000.0)),
                    new eapli.aisafe.weather.domain.WeatherWindSpeed(knots),
                    new eapli.aisafe.weather.domain.WeatherWindDirection(dirDeg),
                    new eapli.aisafe.weather.domain.DataSource("OpenMeteo"));
            data.put(wd.identity(), wd);
            return wd;
        }

        @Override public <S extends WeatherData> S save(final S e) { data.put(e.identity(), e); return e; }
        @Override public Optional<WeatherData> ofIdentity(final WeatherDataId id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean containsOfIdentity(final WeatherDataId id) { return data.containsKey(id); }
        @Override public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode a, final LocalDate d) {
            return data.values().stream().filter(w -> w.areaCode().equals(a)).toList();
        }
        @Override public Iterable<WeatherData> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final WeatherData e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final WeatherDataId id) { data.remove(id); }
    }
}