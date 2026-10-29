package eapli.aisafe.flightmanagement.domain.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftType;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.FuelType;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.makermanagement.domain.Maker;

/**
 * Unit tests for the US085 {@link AircraftPhysics} value object: the mapping from
 * the domain ({@link AircraftModel} + {@link EngineModel}) onto the §3.3 physics
 * inputs (with the pinned unit conversions), and the two-point thrust line and
 * altitude-lapse helpers — which the cruise-dominated fuel burn does not exercise
 * but the spec's climb/descent model relies on.
 */
public class AircraftPhysicsTest {

    private static final double EPS = 1e-9;

    private static AircraftModel a320neo() {
        final Maker airbus = Maker.valueOf("Airbus");
        final EngineModel leap = new EngineModel(EngineModelName.valueOf("CFM LEAP-1A"), airbus,
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
        return new AircraftModel(AircraftModelName.valueOf("A320neo"), airbus, AircraftType.PASSENGER,
                MotorizationType.TURBOFAN, 42600, 79000, 64300, 26730, 12131, 833,
                122.6, 35.8, 0.024, 1.5, 6300, Set.of(leap));
    }

    private static EngineModel leap() {
        return new EngineModel(EngineModelName.valueOf("CFM LEAP-1A"), Maker.valueOf("Airbus"),
                MotorizationType.TURBOFAN, FuelType.JET_A, 130000, 24000, 0.51);
    }

    @Test
    public void ensureFromMapsDomainOntoPhysicsInputsWithPinnedConversions() {
        final AircraftPhysics p = AircraftPhysics.from(a320neo(), leap(), 150);

        assertEquals("A320neo", p.modelName());
        assertEquals(122.6, p.wingAreaM2(), EPS);
        assertEquals(35.8, p.wingSpanM(), EPS);
        assertEquals(35.8 * 35.8 / 122.6, p.aspectRatio(), 1e-6);   // AR = span²/area
        assertEquals(833.0 / 3.6, p.cruiseSpeedMps(), 1e-6);         // km/h → m/s
        assertEquals(0.024, p.cd0(), EPS);                          // dragCoefficient treated as Cd0
        assertEquals(AircraftPhysics.OSWALD_EFFICIENCY, p.oswaldEfficiency(), EPS);
        assertEquals(0.51 / AircraftPhysics.TSFC_TO_N_PER_N_PER_S, p.tsfcNPerNPerS(), EPS); // /3600
        assertEquals(AircraftPhysics.DEFAULT_NUMBER_OF_ENGINES, p.numberOfEngines());
        assertEquals(130000.0, p.staticThrustPerEngineN(), EPS);
        assertEquals(24000.0, p.cruiseThrustPerEngineN(), EPS);
        assertEquals(12131.0, p.serviceCeilingM(), EPS);
        assertEquals(150, p.totalSeats());
    }

    @Test
    public void ensureThrustIsLinearBetweenStaticAndCruiseAndClampsAboveCruise() {
        final AircraftPhysics p = AircraftPhysics.from(a320neo(), leap(), 150);
        final double cruise = p.cruiseSpeedMps();

        assertEquals("static thrust at v=0", 130000.0, p.thrustPerEngineAtSpeed(0.0), EPS);
        assertEquals("cruise thrust at v=cruise", 24000.0, p.thrustPerEngineAtSpeed(cruise), 1e-6);
        assertEquals("midpoint is the average", (130000.0 + 24000.0) / 2.0,
                p.thrustPerEngineAtSpeed(cruise / 2.0), 1e-3);
        assertEquals("clamped to cruise above cruise speed", 24000.0,
                p.thrustPerEngineAtSpeed(cruise * 2.0), 1e-6);
    }

    @Test
    public void ensureThrustGuardsAgainstNonPositiveCruiseSpeed() {
        // Directly construct a degenerate physics with cruiseSpeedMps = 0 to hit the
        // `cruiseSpeedMps <= 0 ? 1.0 : …` guard: the ratio collapses to 1 → cruise thrust.
        final AircraftPhysics degenerate = new AircraftPhysics(
                "X", 100.0, 30.0, 9.0, /*cruiseSpeedMps*/ 0.0, 40000.0, 60000.0, 75000.0,
                12000.0, 0.02, 0.8, 1.4, 120000.0, 20000.0, 2, 1.4e-4, 100);

        assertEquals(20000.0, degenerate.thrustPerEngineAtSpeed(250.0), EPS);
    }

    @Test
    public void ensureAltitudeThrustFactorIsOneAtSeaLevelAndDropsWithAltitude() {
        final AircraftPhysics p = AircraftPhysics.from(a320neo(), leap(), 150);

        assertEquals("(ρ/ρ0)^0.9 = 1 at sea-level density", 1.0,
                p.altitudeThrustFactor(Atmosphere.RHO0), 1e-9);

        final double atCruise = p.altitudeThrustFactor(Atmosphere.density(9000.0));
        assertTrue("thrust lapses below 1 at altitude", atCruise < 1.0 && atCruise > 0.0);
    }
}