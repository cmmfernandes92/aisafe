package eapli.aisafe.flightmanagement.domain.physics;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;

/**
 * The §3.3 physics inputs for one aircraft, assembled from the domain
 * ({@link AircraftModel} + a certified {@link EngineModel}) plus the two physics
 * constants the spec leaves to the model fidelity (Oswald efficiency and the
 * installed-engine count). Used by the US085 fuel-burn model and surfaced to the
 * simulator view so the browser reads its constants from the domain instead of a
 * hard-coded table.
 *
 * <p>Unit handling pinned here (design-notes §2.3):</p>
 * <ul>
 *   <li>cruise speed is stored km/h → converted to m/s ({@code /3.6});</li>
 *   <li>{@code dragCoefficient} is treated as the zero-lift {@code Cd0};</li>
 *   <li>TSFC = stored {@code efficiency / 3600} to reach N/N/s (the stored value
 *       is lb/(lbf·hr), e.g. LEAP ≈ 0.51).</li>
 * </ul>
 */
public record AircraftPhysics(
        String modelName,
        double wingAreaM2,
        double wingSpanM,
        double aspectRatio,
        double cruiseSpeedMps,
        double emptyWeightKg,
        double mzfwKg,
        double mtowKg,
        double serviceCeilingM,
        double cd0,
        double oswaldEfficiency,
        double liftCoefficient,
        double staticThrustPerEngineN,
        double cruiseThrustPerEngineN,
        int numberOfEngines,
        double tsfcNPerNPerS,
        int totalSeats) {

    /** Oswald span-efficiency factor — a class-typical constant (design-notes §2.4a). */
    public static final double OSWALD_EFFICIENCY = 0.8;
    /** Stored TSFC unit (lb/(lbf·hr)) → N/N/s divisor (design-notes §2.3). */
    public static final double TSFC_TO_N_PER_N_PER_S = 3600.0;
    /**
     * Installed-engine count is not stored in the domain (design-notes §2.4b); the
     * seeded models are twins. It only scales climb max-thrust — cruise burn uses
     * thrust = drag, so this assumption does not affect the dominant fuel figure.
     */
    public static final int DEFAULT_NUMBER_OF_ENGINES = 2;

    /**
     * Assembles the physics inputs for a model + one of its certified engines.
     *
     * @param model       the aircraft model (geometry, weights, drag/lift, ceiling)
     * @param engine      a certified engine model (thrust points + TSFC)
     * @param totalSeats  cabin seat count of the specific aircraft (for payload)
     */
    public static AircraftPhysics from(final AircraftModel model, final EngineModel engine,
            final int totalSeats) {
        return new AircraftPhysics(
                model.modelName().toString(),
                model.wingArea(),
                model.wingSpan(),
                model.aspectRatio(),
                model.cruiseSpeed() / 3.6,
                model.emptyWeight(),
                model.mzfw(),
                model.mtow(),
                model.serviceCeiling(),
                model.dragCoefficient(),
                OSWALD_EFFICIENCY,
                model.liftCoefficient(),
                engine.power(),
                engine.cruiseThrust(),
                DEFAULT_NUMBER_OF_ENGINES,
                engine.efficiency() / TSFC_TO_N_PER_N_PER_S,
                totalSeats);
    }

    /**
     * Per-engine thrust at airspeed {@code vMps}, linear between static thrust (v=0)
     * and cruise thrust (v = cruise speed), clamped — the §3.3 two-point thrust line.
     */
    public double thrustPerEngineAtSpeed(final double vMps) {
        final double t = Math.max(0.0, Math.min(1.0, cruiseSpeedMps <= 0 ? 1.0 : vMps / cruiseSpeedMps));
        return staticThrustPerEngineN + (cruiseThrustPerEngineN - staticThrustPerEngineN) * t;
    }

    /** Thrust-altitude lapse factor {@code (ρ/ρ0)^0.9} (spec §3.3, exponent 0.9 not 0.96). */
    public double altitudeThrustFactor(final double density) {
        return Math.pow(density / Atmosphere.RHO0, 0.9);
    }
}