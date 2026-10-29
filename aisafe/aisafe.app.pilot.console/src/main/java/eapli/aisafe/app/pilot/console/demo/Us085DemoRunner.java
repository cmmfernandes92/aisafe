package eapli.aisafe.app.pilot.console.demo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.airtransportcompanymanagement.domain.CompanyIATACode;
import eapli.aisafe.app.pilot.console.presentation.flightplan.ConsoleFlightPlanCompiler;
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
import eapli.aisafe.flightmanagement.application.ImportFlightPlanController;
import eapli.aisafe.flightmanagement.application.SimulatorV1FlightPlanTestService;
import eapli.aisafe.flightmanagement.application.TestFlightPlanController;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.repositories.FlightRepository;
import eapli.aisafe.flightplanmanagement.dsl.CompilationResult;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporter;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporterFactory;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.pilotmanagement.domain.Pilot;
import eapli.aisafe.usermanagement.domain.AisafePasswordPolicy;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.aisafe.usermanagement.domain.Email;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

/**
 * US085 end-to-end demonstration runner (single process, in-memory, no remote DB).
 *
 * <p>It scripts the full pilot scenario through the real production controllers and
 * the real ANTLR DSL compiler: it registers a pilot user for Ryanair (FR), seeds an
 * FR A320neo aircraft, creates an FR401 flight, imports an actual {@code .flight}
 * sample (US121, lexical/syntactic/semantic validation), and runs the US085
 * test/validate use case, printing what was created and the verdict. It exists so the
 * US085 documentation can show a real run without driving the interactive console
 * against the shared remote database. Repositories are simple in-memory fakes so the
 * run needs no database at all.</p>
 */
public final class Us085DemoRunner {

    private static final String PILOT_EMAIL = "pilot@aisafe.test";
    private static final String SAMPLE = "FR401-OPO-LIS.flight";

    private Us085DemoRunner() {
    }

    public static void main(final String[] args) throws Exception {
        line();
        System.out.println("US085 end-to-end demonstration (in-memory, real controllers + real DSL compiler)");
        line();

        // 1) Authentication: register a pilot user and log in (the pilot console does
        //    this at startup).
        final var authUsers = new InMemoryUserRepository();
        authUsers.save(new SystemUserBuilder(new AisafePasswordPolicy(), new PlainTextEncoder())
                .withUsername("demo.pilot").withPassword("Password1").withName("Demo", "Pilot")
                .withEmail(PILOT_EMAIL).withRoles(AisafeRoles.PILOT).build());
        AuthzRegistry.configure(authUsers, new AisafePasswordPolicy(), new PlainTextEncoder());
        AuthzRegistry.authenticationService().authenticate("demo.pilot", "Password1", AisafeRoles.PILOT);
        System.out.println("[1] Pilot user registered and authenticated: demo.pilot <" + PILOT_EMAIL + "> role=PILOT");

        // 2) Reference data + the FR pilot and aircraft.
        final var flights = new FakeFlightRepository();
        final var aircraft = new FakeAircraftRepository();
        final var models = new FakeAircraftModelRepository();
        final var weather = new FakeWeatherDataRepository();

        final Maker airbus = Maker.valueOf("Airbus");
        final EngineModel leap = new EngineModel(EngineModelName.valueOf("CFM LEAP-1A"), airbus,
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
        final AircraftModel a320 = new AircraftModel(AircraftModelName.valueOf("A320neo"), airbus,
                AircraftType.PASSENGER, MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833,
                122.6, 35.8, 0.024, 1.5, 6300, Set.of(leap));
        final long modelId = models.saveAndGetId(a320);

        final Registration reg = Registration.valueOf("CS-TUA");
        aircraft.save(new Aircraft(reg, modelId, CompanyIATACode.valueOf("FR"),
                new CabinConfiguration(Map.of(SeatClass.ECONOMY, 150)), new Country("Portugal"), 0));

        final Email pilotEmail = Email.valueOf(PILOT_EMAIL);
        final Pilot pilot = new Pilot(pilotEmail, CompanyIATACode.valueOf("FR"), Set.of(a320));
        System.out.println("[2] Added pilot to FR (Ryanair): " + pilot.identity()
                + ", certified for A320neo; registered FR aircraft " + reg + " (A320neo, modelId=" + modelId + ").");

        // 3) Create the FR401 flight (departure matches the sample's first leg so the
        //    US085 departure-consistency check holds).
        final RouteName fr401 = RouteName.valueOf("FR401");
        final LocalDateTime departure = LocalDateTime.parse("2026-06-12T13:00");
        final Flight flight = new Flight(fr401, pilotEmail, reg, departure);
        flights.save(flight);
        final FlightId flightId = flight.identity();
        System.out.println("[3] Created flight " + flightId + " on route FR401, aircraft " + reg
                + ", departure " + departure + "; initial plan status = " + flight.currentPlan().status());

        // 4) US121 - import the real .flight sample (lexical/syntactic/semantic validation).
        final Path file = sampleFile();
        final String raw = Files.readString(file);
        final FlightPlanImporter importer = FlightPlanImporterFactory.forPath(file).orElseThrow();
        final CompilationResult compilation;
        try (InputStream in = Files.newInputStream(file)) {
            compilation = importer.importFrom(in, file.getFileName().toString());
        }
        System.out.println("[4] US121 import of " + file.getFileName()
                + " -> DSL validation " + (compilation.ok() ? "OK" : "FAILED " + compilation.errors()));
        final ImportFlightPlanController importController = new ImportFlightPlanController(flights);
        final Flight imported = importController.importFlightPlan(flightId, raw);
        System.out.println("    plan status after import = " + imported.currentPlan().status());

        // 5) US085 - test/validate the plan (real ANTLR recompile + §3.3 physics).
        final TestFlightPlanController testController = new TestFlightPlanController(flights,
                new SimulatorV1FlightPlanTestService(new ConsoleFlightPlanCompiler(), weather, aircraft, models));
        final FlightPlanTestResult result = testController.testFlightPlan(flightId);

        line();
        System.out.println("[5] US085 result: " + (result.isSuccess() ? "PASSED" : "FAILED"));
        System.out.println("    " + result.message());
        System.out.println("    final plan status = "
                + flights.ofIdentity(flightId).orElseThrow().currentPlan().status());
        line();
    }

    private static Path sampleFile() {
        for (final String candidate : new String[] {
                "../docs/sprint-3/samples/" + SAMPLE,
                "docs/sprint-3/samples/" + SAMPLE,
                "../../docs/sprint-3/samples/" + SAMPLE }) {
            final Path p = Path.of(candidate);
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new IllegalStateException("Could not locate sample " + SAMPLE
                + " relative to " + Path.of("").toAbsolutePath());
    }

    private static void line() {
        System.out.println("------------------------------------------------------------------------");
    }

    // ---- in-memory fake repositories (no database needed) ----

    private static final class FakeFlightRepository implements FlightRepository {
        private final Map<FlightId, Flight> data = new HashMap<>();

        @Override
        public boolean existsForRouteOnOrAfter(eapli.aisafe.flightroutemanagement.domain.RouteName route, java.time.LocalDate date) {
            return false;
        }

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
        @Override public Iterable<WeatherData> findByAreaCodeAndDay(final AreaCode a, final LocalDate d) {
            return data.values().stream().filter(w -> w.areaCode().equals(a)).toList();
        }
        @Override public Iterable<WeatherData> findAll() { return data.values(); }
        @Override public long count() { return data.size(); }
        @Override public void delete(final WeatherData e) { data.remove(e.identity()); }
        @Override public void deleteOfIdentity(final WeatherDataId id) { data.remove(id); }
    }
}