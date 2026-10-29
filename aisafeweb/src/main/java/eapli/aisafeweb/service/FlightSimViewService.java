package eapli.aisafeweb.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import eapli.aisafe.flightmanagement.application.SimulateFlightPlanController;
import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightmanagement.domain.physics.AircraftPhysics;
import eapli.aisafe.flightmanagement.domain.physics.FlightSimulationModel;
import eapli.aisafe.flightmanagement.domain.physics.ResolvedSegment;
import eapli.aisafe.flightroutemanagement.domain.RouteName;
import eapli.aisafe.infrastructure.persistence.PersistenceContext;
import eapli.aisafeweb.dsl.DslFlightPlanCompiler;

/**
 * Builds the browser sim's plan view-model by delegating to the core US085
 * {@link SimulateFlightPlanController}: the recompile (via the {@link DslFlightPlanCompiler}
 * port), the per-segment weather resolution and the §3.3 physics all run in
 * {@code aisafe.core}. This service is now a thin adapter that maps the core
 * {@link FlightSimulationModel} onto the JSON shape the client expects — it no
 * longer carries its own physics constants or reads DSL wind directly.
 *
 * <p>So the simulator runs a real, weather-resolved, physics-tested plan, reading
 * its numbers from the domain (design-notes §2.5 / §3.2). Read-only — it never
 * changes plan state; the {@code VALIDATED → TESTED} transition is the US085
 * test use case (console / web test action), not this view.</p>
 */
@Service
public class FlightSimViewService {

    private final DslFlightPlanCompiler compiler;

    public FlightSimViewService(final DslFlightPlanCompiler compiler) {
        this.compiler = compiler;
    }

    // ── View model (client JSON contract) ────────────────────────────────────

    public record SimPlanView(
            String designator,
            String originCode, String destinationCode,
            String departureIso, String arrivalIso,
            double fuelKg,
            double tripBurnKg,
            double requiredFuelKg,
            boolean testPassed,
            String testSummary,
            List<Seg> segments,
            Ac aircraft) {

        public record Seg(double startLat, double startLon, double endLat, double endLon,
                double altitudeM, double windDirDeg, double windSpeedMs, String windSource) {
        }

        public record Ac(String model, double wingArea, double cruiseSpeedMs,
                double emptyWeightKg, double mzfwKg, double mtowKg, double ceilingM,
                int totalSeats, double thrustMaxN, double cruiseThrustN, int numEngines, double tsfcNNs,
                double cd0, double aspectRatio, double oswaldE,
                double dragCoefficient, double liftCoefficient) {
        }
    }

    /**
     * The view-model for a pilot's own VALIDATED/TESTED flight, or empty if it
     * doesn't exist, isn't theirs, has no DSL yet, or fails to recompile.
     */
    public Optional<SimPlanView> buildFor(final String pilotEmail, final String route, final LocalDate date) {
        if (pilotEmail == null || route == null || date == null) {
            return Optional.empty();
        }
        final SimulateFlightPlanController controller = new SimulateFlightPlanController(
                PersistenceContext.repositories().flights(),
                compiler,
                PersistenceContext.repositories().weatherData(),
                PersistenceContext.repositories().aircraft(),
                PersistenceContext.repositories().aircraftModels());

        return controller.simulateFor(pilotEmail, new FlightId(RouteName.valueOf(route), date))
                .filter(model -> model.aircraft() != null && !model.segments().isEmpty())
                .map(FlightSimViewService::toView);
    }

    private static SimPlanView toView(final FlightSimulationModel model) {
        final List<SimPlanView.Seg> segs = model.segments().stream()
                .map(FlightSimViewService::toSeg)
                .toList();
        return new SimPlanView(
                model.designator(), model.originCode(), model.destinationCode(),
                model.departureIso(), model.arrivalIso(),
                model.loadedFuelKg(), model.tripBurnKg(), model.requiredFuelKg(),
                model.passed(), model.verdict() == null ? "" : model.verdict().summary(),
                segs, toAc(model.aircraft()));
    }

    private static SimPlanView.Seg toSeg(final ResolvedSegment s) {
        return new SimPlanView.Seg(
                s.startLatitude(), s.startLongitude(), s.endLatitude(), s.endLongitude(),
                s.altitudeMeters(), s.windDirectionDeg(), s.windSpeedMps(), s.windSource().name());
    }

    private static SimPlanView.Ac toAc(final AircraftPhysics a) {
        return new SimPlanView.Ac(
                a.modelName(), a.wingAreaM2(), a.cruiseSpeedMps(),
                a.emptyWeightKg(), a.mzfwKg(), a.mtowKg(), a.serviceCeilingM(),
                a.totalSeats(), a.staticThrustPerEngineN(), a.cruiseThrustPerEngineN(),
                a.numberOfEngines(), a.tsfcNPerNPerS(),
                a.cd0(), a.aspectRatio(), a.oswaldEfficiency(),
                a.cd0(), a.liftCoefficient());
    }
}