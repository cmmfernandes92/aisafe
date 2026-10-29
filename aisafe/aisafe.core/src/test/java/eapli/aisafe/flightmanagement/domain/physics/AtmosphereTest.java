package eapli.aisafe.flightmanagement.domain.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** ISA atmosphere sanity checks (US085 §3.3). */
public class AtmosphereTest {

    private static final double TOL = 0.02;

    @Test
    public void seaLevelDensityIsAboutStandard() {
        assertEquals(1.225, Atmosphere.density(0.0), TOL);
        assertEquals(Atmosphere.RHO0, Atmosphere.density(0.0), 1e-9);
    }

    @Test
    public void densityDecreasesWithAltitude() {
        assertTrue(Atmosphere.density(0.0) > Atmosphere.density(3000.0));
        assertTrue(Atmosphere.density(3000.0) > Atmosphere.density(9000.0));
    }

    @Test
    public void densityAt9000mMatchesIsaTable() {
        // ISA density at 9 km ≈ 0.4671 kg/m³
        assertEquals(0.467, Atmosphere.density(9000.0), 0.01);
    }

    @Test
    public void temperatureFollowsTroposphericLapse() {
        assertEquals(Atmosphere.T0_K, Atmosphere.temperatureK(0.0), 1e-9);
        assertEquals(288.15 - 0.0065 * 9000.0, Atmosphere.temperatureK(9000.0), 1e-9);
    }

    @Test
    public void speedOfSoundDropsWithAltitude() {
        assertTrue(Atmosphere.speedOfSound(0.0) > Atmosphere.speedOfSound(9000.0));
        assertEquals(340.3, Atmosphere.speedOfSound(0.0), 0.5);
    }
}