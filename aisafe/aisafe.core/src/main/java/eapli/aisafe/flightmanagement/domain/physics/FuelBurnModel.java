package eapli.aisafe.flightmanagement.domain.physics;

import java.util.ArrayList;
import java.util.List;

/**
 * The §3.3 fuel-burn model: an Euler integration of {@code burn = P·Δt·TSFC/g}
 * over the route, identical in formula to the browser reference
 * ({@code simulator-flight.js}) so the core test and the simulator agree.
 *
 * <p>Per step, at the step's ISA density and constant cruise TAS:
 * {@code CL = 2mg/(ρAv²)}, {@code CD = Cd0 + CL²/(πARe)}, {@code D = ½ρv²A·CD}; in
 * level flight thrust balances drag so {@code P = D}. The two-point thrust
 * model and altitude lapse only bite in climb/descent, where they barely move the
 * cruise-dominated total — so the fuel verdict is robust to thrust-model
 * uncertainty (design-notes §2.1). Climb/descent are modelled as eased altitude
 * ramps over the first/last {@value #RAMP_FRACTION} of the route.</p>
 */
public final class FuelBurnModel {

    /** Per-seat payload estimate (pax + bags), matching the JS reference. */
    public static final double PAX_MASS_KG = 100.0;
    /** Share of the route used for the climb and descent altitude ramps. */
    public static final double RAMP_FRACTION = 0.12;
    /** Number of Euler steps (matches the JS reference's resolution). */
    public static final int STEPS = 240;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private FuelBurnModel() {
    }

    /**
     * Integrates the trip fuel burn over the resolved route.
     *
     * @param aircraft     the §3.3 physics inputs
     * @param segments     the resolved segments, in route order (geometry + altitude)
     * @param loadedFuelKg fuel loaded per the DSL (kg)
     * @param blockSeconds scheduled block time, arrival − departure (s); a sane
     *                     floor is applied if non-positive
     * @return the burn result (trip burn, remaining fuel, timeline)
     */
    public static FuelBurnResult integrate(final AircraftPhysics aircraft,
            final List<ResolvedSegment> segments, final double loadedFuelKg, final double blockSeconds) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("At least one resolved segment is required");
        }
        final double[] cumulative = cumulativeLengths(segments);
        final double totalLength = cumulative[cumulative.length - 1];
        final double seconds = blockSeconds > 0 ? blockSeconds : Math.max(60.0, segments.size() * 600.0);
        final double dt = seconds / STEPS;
        final double v = aircraft.cruiseSpeedMps();

        final double payload = Math.min(aircraft.totalSeats() * PAX_MASS_KG,
                Math.max(0.0, aircraft.mzfwKg() - aircraft.emptyWeightKg()));
        double mass = aircraft.emptyWeightKg() + payload + loadedFuelKg;
        double fuel = loadedFuelKg;

        final List<FuelBurnResult.Sample> timeline = new ArrayList<>(STEPS + 1);
        timeline.add(new FuelBurnResult.Sample(0.0, fuel, 0.0));

        for (int i = 1; i <= STEPS; i++) {
            final double p = (double) i / STEPS;
            final double altM = altitudeAt(p, segments, cumulative, totalLength);
            final double rho = Atmosphere.density(altM);
            final double cl = (2.0 * mass * Atmosphere.G) / (rho * aircraft.wingAreaM2() * v * v);
            final double cd = aircraft.cd0()
                    + (cl * cl) / (Math.PI * aircraft.aspectRatio() * aircraft.oswaldEfficiency());
            final double drag = 0.5 * rho * v * v * aircraft.wingAreaM2() * cd;
            final double burn = (drag * dt * aircraft.tsfcNPerNPerS()) / Atmosphere.G;
            // fuel is tracked true (it may go negative — that is the trip needing more
            // than was loaded, surfaced honestly as the trip burn); only the displayed
            // timeline is floored at 0 so the HUD shows an empty tank, not a deficit.
            fuel -= burn;
            mass -= burn;
            timeline.add(new FuelBurnResult.Sample(p, Math.max(0.0, fuel), altM));
        }

        final double tripBurn = loadedFuelKg - fuel; // true integrated burn (not capped at loaded)
        return new FuelBurnResult(loadedFuelKg, tripBurn, fuel, seconds, timeline);
    }

    /** Cruise altitude at route fraction {@code p}, with eased climb/descent ramps. */
    private static double altitudeAt(final double p, final List<ResolvedSegment> segments,
            final double[] cumulative, final double totalLength) {
        final double firstAlt = segments.get(0).altitudeMeters();
        final double lastAlt = segments.get(segments.size() - 1).altitudeMeters();
        if (p <= 0) {
            return Math.max(1.0, firstAlt * easeInOut(0));
        }
        if (p < RAMP_FRACTION) {
            return Math.max(1.0, firstAlt * easeInOut(p / RAMP_FRACTION));
        }
        if (p > 1 - RAMP_FRACTION) {
            return Math.max(1.0, lastAlt * easeInOut(Math.max(0, (1 - p) / RAMP_FRACTION)));
        }
        return segments.get(segmentIndexAt(p, cumulative, totalLength)).altitudeMeters();
    }

    /** Index of the segment whose cumulative-length bucket contains route fraction {@code p}. */
    private static int segmentIndexAt(final double p, final double[] cumulative, final double totalLength) {
        final double d = Math.max(0, Math.min(totalLength, p * totalLength));
        for (int i = 0; i < cumulative.length - 1; i++) {
            if (d <= cumulative[i + 1] + 1e-9) {
                return i;
            }
        }
        return cumulative.length - 2;
    }

    /** Cumulative great-circle distance (m) along the segment polyline; index 0 = 0. */
    private static double[] cumulativeLengths(final List<ResolvedSegment> segments) {
        final double[] cumulative = new double[segments.size() + 1];
        cumulative[0] = 0.0;
        for (int i = 0; i < segments.size(); i++) {
            final ResolvedSegment s = segments.get(i);
            final double len = haversineMeters(s.startLatitude(), s.startLongitude(),
                    s.endLatitude(), s.endLongitude());
            cumulative[i + 1] = cumulative[i] + Math.max(1.0, len);
        }
        return cumulative;
    }

    private static double haversineMeters(final double lat1, final double lon1,
            final double lat2, final double lon2) {
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLon = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    private static double easeInOut(final double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }
}