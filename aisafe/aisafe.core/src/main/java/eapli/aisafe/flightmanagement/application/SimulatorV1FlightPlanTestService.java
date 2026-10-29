package eapli.aisafe.flightmanagement.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.domain.Flight;
import eapli.aisafe.flightmanagement.domain.FlightPlan;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestResult;
import eapli.aisafe.flightmanagement.domain.FlightPlanTestService;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightmanagement.domain.physics.AircraftPhysics;
import eapli.aisafe.flightmanagement.domain.physics.FlightPlanTestVerdict;
import eapli.aisafe.flightmanagement.domain.physics.FlightSimulationModel;
import eapli.aisafe.flightmanagement.domain.physics.FuelBurnModel;
import eapli.aisafe.flightmanagement.domain.physics.FuelBurnResult;
import eapli.aisafe.flightmanagement.domain.physics.ResolvedSegment;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;
import eapli.framework.validations.Preconditions;

/**
 * The production {@link FlightPlanTestService} for US085 (Simulator&nbsp;v1): it
 * recompiles the plan's DSL via the injected {@link FlightPlanCompiler} port,
 * overlays the pilot's attached weather (US082, DSL wind as fallback), and runs
 * the §3.3 physics to decide whether the plan passes — fuel sufficiency,
 * minimum-altitude compliance, and the departure-time consistency check (PO
 * 02/06/2026). A failing test returns a clear, reasoned failure and leaves plan
 * state untouched (US085.5); the {@code VALIDATED → TESTED} promotion is driven by
 * {@code Flight.testCurrentPlan} only on success.
 *
 * <p>It stays {@code aisafe.core}-internal and DSL-free: the DSL parse is provided
 * by the {@link FlightPlanCompiler} adapter (console/web), mirroring the weather
 * {@code WeatherDataSourceReader} port. {@link #simulate(Flight, FlightPlan)}
 * exposes the same computation read-only (no state change) for the simulator view.</p>
 */
public class SimulatorV1FlightPlanTestService implements FlightPlanTestService {

    /** Contingency fraction of the trip burn held as reserve (design-notes / spec). */
    public static final double RESERVE_CONTINGENCY_FRACTION = 0.10;
    /** Fixed final reserve added on top of the contingency (kg). */
    public static final double RESERVE_FIXED_KG = 600.0;
    /** JET A-1 density at 15°C (spec §3.3) — only for the DSL litre→kg conversion. */
    public static final double FUEL_DENSITY_KG_PER_L = 0.804;

    private final FlightPlanCompiler compiler;
    private final WeatherDataRepository weatherRepository;
    private final AircraftRepository aircraftRepository;
    private final AircraftModelRepository aircraftModelRepository;

    public SimulatorV1FlightPlanTestService(final FlightPlanCompiler compiler,
            final WeatherDataRepository weatherRepository,
            final AircraftRepository aircraftRepository,
            final AircraftModelRepository aircraftModelRepository) {
        Preconditions.noneNull(compiler, weatherRepository, aircraftRepository, aircraftModelRepository);
        this.compiler = compiler;
        this.weatherRepository = weatherRepository;
        this.aircraftRepository = aircraftRepository;
        this.aircraftModelRepository = aircraftModelRepository;
    }

    @Override
    public FlightPlanTestResult test(final Flight flight, final FlightPlan plan) {
        final FlightSimulationModel model = simulate(flight, plan);
        return model.passed()
                ? FlightPlanTestResult.success(model.verdict().summary())
                : FlightPlanTestResult.failure(model.verdict().summary());
    }

    /**
     * Runs the full simulation read-only and returns the result model (resolved
     * winds, physics inputs, fuel timeline, verdict). Never changes plan state.
     */
    public FlightSimulationModel simulate(final Flight flight, final FlightPlan plan) {
        Preconditions.noneNull(flight, plan);

        final FlightPlanCompilationResult compilation = compiler.compile(plan.rawDslContent());
        if (!compilation.ok()) {
            final List<String> issues = compilation.issues().stream().map(i -> i.describe()).toList();
            return failedToCompile(issues);
        }

        final ParsedFlightPlan parsed = compilation.plan().orElseThrow();
        final Map<SegmentKey, WeatherDataId> assignments = plan.weatherAssignmentsBySegment();
        final List<ResolvedSegment> segments = new WindResolver(weatherRepository).resolve(parsed, assignments);

        final AircraftPhysics aircraft = assembleAircraft(flight);
        final double loadedFuelKg = toKilograms(parsed.fuelQuantity(), parsed.fuelUnit());
        final double blockSeconds = Duration.between(parsed.departure(), parsed.arrival()).getSeconds();
        final FuelBurnResult burn = FuelBurnModel.integrate(aircraft, segments, loadedFuelKg, blockSeconds);
        final double requiredFuelKg = burn.tripBurnKg() * (1.0 + RESERVE_CONTINGENCY_FRACTION) + RESERVE_FIXED_KG;

        final FlightPlanTestVerdict verdict =
                evaluate(flight, parsed, segments, aircraft, loadedFuelKg, burn, requiredFuelKg);

        return new FlightSimulationModel(
                parsed.designator(), parsed.originCode(), parsed.destinationCode(),
                parsed.departure().toString(), parsed.arrival().toString(),
                loadedFuelKg, burn.tripBurnKg(), requiredFuelKg,
                segments, aircraft, burn, verdict);
    }

    private FlightPlanTestVerdict evaluate(final Flight flight, final ParsedFlightPlan parsed,
            final List<ResolvedSegment> segments, final AircraftPhysics aircraft,
            final double loadedFuelKg, final FuelBurnResult burn, final double requiredFuelKg) {
        final List<String> failures = new ArrayList<>();

        // C. Departure-time consistency (PO 02/06/2026 duplication check) — exact match.
        if (!flight.departure().equals(parsed.departure())) {
            failures.add("Departure mismatch: Flight entity " + flight.departure()
                    + " != first-leg DSL departure " + parsed.departure() + ".");
        }

        // B. Minimum-altitude compliance: 0 < altitude <= service ceiling.
        for (final ResolvedSegment s : segments) {
            final double a = s.altitudeMeters();
            if (a <= 0) {
                failures.add("Altitude " + fmt(a) + " m on leg " + s.legNumber()
                        + " segment " + s.segmentIndex() + " must be positive.");
            } else if (a > aircraft.serviceCeilingM()) {
                failures.add("Altitude " + fmt(a) + " m on leg " + s.legNumber()
                        + " segment " + s.segmentIndex() + " exceeds service ceiling "
                        + fmt(aircraft.serviceCeilingM()) + " m.");
            }
        }

        // A. Fuel sufficiency: loaded >= trip burn + 10% contingency + 600 kg reserve.
        if (loadedFuelKg < requiredFuelKg) {
            failures.add("Insufficient fuel: loaded " + fmt(loadedFuelKg) + " kg < required "
                    + fmt(requiredFuelKg) + " kg (trip burn " + fmt(burn.tripBurnKg())
                    + " kg + 10% contingency + " + fmt(RESERVE_FIXED_KG) + " kg reserve).");
        }

        if (!failures.isEmpty()) {
            return FlightPlanTestVerdict.failed(failures);
        }
        return FlightPlanTestVerdict.passed("Plan tested OK for " + parsed.designator()
                + " — trip burn " + fmt(burn.tripBurnKg()) + " kg, loaded " + fmt(loadedFuelKg)
                + " kg (margin " + fmt(loadedFuelKg - requiredFuelKg) + " kg); all "
                + segments.size() + " segment altitudes within the service ceiling; departure consistent.");
    }

    private AircraftPhysics assembleAircraft(final Flight flight) {
        final Aircraft aircraft = aircraftRepository.ofIdentity(flight.aircraft())
                .orElseThrow(() -> new IllegalStateException(
                        "Aircraft " + flight.aircraft() + " is not registered; cannot resolve physics."));
        final AircraftModel model = aircraftModelRepository.ofIdentity(aircraft.modelId())
                .orElseThrow(() -> new IllegalStateException(
                        "Aircraft model " + aircraft.modelId() + " not found; cannot resolve physics."));
        // Deterministic pick: the Aircraft doesn't record which certified engine is
        // installed, so choose the first by name so the test is reproducible (a Set's
        // iteration order is not guaranteed).
        final EngineModel engine = model.certifiedEngines().stream()
                .min(java.util.Comparator.comparing(e -> e.engineModelName().toString()))
                .orElseThrow(() -> new IllegalStateException(
                        "Aircraft model " + model.modelName() + " has no certified engine."));
        return AircraftPhysics.from(model, engine, aircraft.cabinConfiguration().totalSeats());
    }

    private static double toKilograms(final double quantity, final ParsedFlightPlan.FuelUnit unit) {
        return unit == ParsedFlightPlan.FuelUnit.L ? quantity * FUEL_DENSITY_KG_PER_L : quantity;
    }

    private FlightSimulationModel failedToCompile(final List<String> issues) {
        final List<String> failures = issues.isEmpty()
                ? List.of("Flight plan DSL could not be compiled.") : issues;
        return new FlightSimulationModel("(invalid)", "", "", "", "", 0, 0, 0,
                List.of(), null, null, FlightPlanTestVerdict.failed(failures));
    }

    private static String fmt(final double v) {
        return String.format(java.util.Locale.ROOT, "%.0f", v);
    }
}