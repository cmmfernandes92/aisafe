package eapli.aisafe.flightmanagement.domain.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Validates the §3.3 fuel-burn integration against the FR401 reference scenario
 * (A320neo, Porto→Lisbon, ~50 min at 9000 m), and confirms the calibrated reserve
 * (trip burn · 1.10 + 600 kg) leaves the seeded 3300 kg load passing.
 */
public class FuelBurnModelTest {

    /** A320neo + LEAP-1A physics, matching the seeded bootstrap values. */
    private static AircraftPhysics a320neo() {
        return new AircraftPhysics(
                "A320neo", 122.6, 35.8, 35.8 * 35.8 / 122.6, 833.0 / 3.6,
                42600, 64300, 79000, 12131, 0.024, AircraftPhysics.OSWALD_EFFICIENCY, 1.5,
                130000, 24000, 2, 0.51 / 3600.0, 150);
    }

    private static List<ResolvedSegment> fr401Segments() {
        return List.of(
                new ResolvedSegment(1, 0, 41.2356, -8.6781, 40.0100, -8.9000, 9000,
                        350, 8.0, WindSource.DSL_BASELINE),
                new ResolvedSegment(1, 1, 40.0100, -8.9000, 38.7742, -9.1342, 9000,
                        340, 6.5, WindSource.DSL_BASELINE));
    }

    @Test
    public void fr401TripBurnIsRealisticAndLeavesMargin() {
        final FuelBurnResult r = FuelBurnModel.integrate(a320neo(), fr401Segments(), 3300.0, 3000.0);

        // A short-haul A320neo trip burn sits in the low thousands of kg.
        assertTrue("trip burn should be positive", r.tripBurnKg() > 0);
        assertTrue("trip burn " + r.tripBurnKg() + " kg out of expected 1000–2600 range",
                r.tripBurnKg() > 1000 && r.tripBurnKg() < 2600);

        // Calibrated reserve: loaded 3300 kg must cover burn + 10% + 600 kg.
        final double required = r.tripBurnKg() * 1.10 + 600.0;
        assertTrue("3300 kg should pass; required was " + required + " kg", 3300.0 >= required);
    }

    @Test
    public void fuelRemainingEqualsLoadedMinusTripBurn() {
        final FuelBurnResult r = FuelBurnModel.integrate(a320neo(), fr401Segments(), 3300.0, 3000.0);
        assertEquals(3300.0 - r.tripBurnKg(), r.fuelRemainingKg(), 1e-6);
    }

    @Test
    public void timelineSpansTheWholeFlight() {
        final FuelBurnResult r = FuelBurnModel.integrate(a320neo(), fr401Segments(), 3300.0, 3000.0);
        assertEquals(FuelBurnModel.STEPS + 1, r.timeline().size());
        assertEquals(0.0, r.timeline().get(0).progress(), 1e-9);
        assertEquals(1.0, r.timeline().get(r.timeline().size() - 1).progress(), 1e-9);
        // fuel is monotonically non-increasing
        for (int i = 1; i < r.timeline().size(); i++) {
            assertTrue(r.timeline().get(i).fuelKg() <= r.timeline().get(i - 1).fuelKg() + 1e-9);
        }
    }

    @Test
    public void lowCruiseAltitudeBurnsFarMoreThanHighCruise() {
        // The FR401 weather plan cruises low (600-3500 m) to stay in Open-Meteo coverage.
        // Same route at 9000 m for comparison — fixed cruise TAS means low altitude = denser
        // air = far more drag = far more burn. This is §3.3 behaviour, not a defect.
        final List<ResolvedSegment> lowProfile = List.of(
                seg(0, 41.2356, -8.6781, 40.9500, -9.1000, 600),
                seg(1, 40.9500, -9.1000, 40.5500, -9.6500, 1500),
                seg(2, 40.5500, -9.6500, 40.1000, -9.9500, 3500),
                seg(3, 40.1000, -9.9500, 39.5500, -9.7000, 3500),
                seg(4, 39.5500, -9.7000, 39.1000, -9.3500, 1500),
                seg(5, 39.1000, -9.3500, 38.7742, -9.1342, 600));
        final List<ResolvedSegment> highProfile = lowProfile.stream()
                .map(s -> new ResolvedSegment(s.legNumber(), s.segmentIndex(), s.startLatitude(),
                        s.startLongitude(), s.endLatitude(), s.endLongitude(), 9000,
                        s.windDirectionDeg(), s.windSpeedMps(), s.windSource()))
                .toList();

        final double lowBurn = FuelBurnModel.integrate(a320neo(), lowProfile, 4700, 3600).tripBurnKg();
        final double highBurn = FuelBurnModel.integrate(a320neo(), highProfile, 4700, 3600).tripBurnKg();

        assertTrue("low-altitude cruise should burn much more", lowBurn > highBurn * 1.5);
        assertTrue("a 9000 m version should be a realistic short-haul burn", highBurn < 2800);

        // trip burn barely moves with loaded fuel (induced drag is a small share here):
        // proves "required" is plan-intrinsic, not a function of how much you load.
        final double burnLight = FuelBurnModel.integrate(a320neo(), lowProfile, 3600, 3600).tripBurnKg();
        assertEquals("burn should be ~independent of loaded fuel", lowBurn, burnLight, 25.0);
    }

    private static ResolvedSegment seg(final int idx, final double sLat, final double sLon,
            final double eLat, final double eLon, final double altM) {
        return new ResolvedSegment(1, idx, sLat, sLon, eLat, eLon, altM, 180, 8.0, WindSource.DSL_BASELINE);
    }

    @Test
    public void heavierFuelLoadBurnsMore() {
        final double burnLight = FuelBurnModel.integrate(a320neo(), fr401Segments(), 2000.0, 3000.0).tripBurnKg();
        final double burnHeavy = FuelBurnModel.integrate(a320neo(), fr401Segments(), 6000.0, 3000.0).tripBurnKg();
        // more mass ⇒ more induced drag ⇒ more burn
        assertTrue(burnHeavy > burnLight);
    }
}