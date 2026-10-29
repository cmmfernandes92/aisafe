package eapli.aisafe.flightmanagement.domain.physics;

/**
 * International Standard Atmosphere (ISA) — air density, temperature, pressure and
 * speed of sound as a function of geometric altitude. These feed the §3.3 lift/drag
 * and thrust-lapse terms used by the US085 flight-plan test.
 *
 * <p>Uses the exact ISA constants ({@code T0 = 288.15 K}, {@code p0 = 101325 Pa},
 * tropospheric lapse {@code 0.0065 K/m}, {@code R = 287 J/(kg·K)}, {@code γ = 1.4}).
 * As the authoritative US085 test it uses exact ISA rather than the browser
 * prototype's rounded {@code 288.2} — the difference in ρ at cruise is &lt;0.1%.
 * Valid through the stratosphere; our cruise altitudes (≤ ~12 km) sit in the
 * troposphere.</p>
 */
public final class Atmosphere {

    /** Sea-level standard temperature (K) — exact ISA. */
    public static final double T0_K = 288.15;
    /** Sea-level standard pressure (Pa). */
    public static final double P0_PA = 101325.0;
    /** Tropospheric temperature lapse rate (K/m). */
    public static final double LAPSE_K_PER_M = 0.0065;
    /** Specific gas constant for dry air (J/(kg·K)). */
    public static final double R = 287.0;
    /** Ratio of specific heats for air. */
    public static final double GAMMA = 1.4;
    /** Tropopause altitude (m). */
    public static final double TROPOPAUSE_M = 11000.0;
    /** Standard gravity (m/s²). */
    public static final double G = 9.81;
    /** Sea-level standard density (kg/m³), derived from {@code p0/(R·T0)}. */
    public static final double RHO0 = P0_PA / (R * T0_K);

    private static final double PRESSURE_EXPONENT = 5.2561;

    private Atmosphere() {
    }

    /** Air temperature (K) at the given geometric altitude (m). */
    public static double temperatureK(final double altitudeMeters) {
        if (altitudeMeters <= TROPOPAUSE_M) {
            return T0_K - LAPSE_K_PER_M * altitudeMeters;
        }
        return 216.65; // isothermal lower stratosphere
    }

    /** Air pressure (Pa) at the given geometric altitude (m). */
    public static double pressurePa(final double altitudeMeters) {
        if (altitudeMeters <= TROPOPAUSE_M) {
            return P0_PA * Math.pow(1.0 - LAPSE_K_PER_M * altitudeMeters / T0_K, PRESSURE_EXPONENT);
        }
        final double tTrop = 216.65;
        final double pTrop = P0_PA * Math.pow(1.0 - LAPSE_K_PER_M * TROPOPAUSE_M / T0_K, PRESSURE_EXPONENT);
        return pTrop * Math.exp(-G * (altitudeMeters - TROPOPAUSE_M) / (R * tTrop));
    }

    /** Air density (kg/m³) at the given geometric altitude (m): {@code ρ = p/(R·T)}. */
    public static double density(final double altitudeMeters) {
        return pressurePa(altitudeMeters) / (R * temperatureK(altitudeMeters));
    }

    /** Speed of sound (m/s) at the given geometric altitude (m): {@code a = √(γ·R·T)}. */
    public static double speedOfSound(final double altitudeMeters) {
        return Math.sqrt(GAMMA * R * temperatureK(altitudeMeters));
    }
}