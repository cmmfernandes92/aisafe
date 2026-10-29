package eapli.aisafe.flightmanagement.domain.physics;

/**
 * Where a segment's resolved wind came from (US085 wind resolution).
 *
 * <ul>
 *   <li>{@link #ATTACHED_FORECAST} — the pilot attached a {@code WeatherData}
 *       forecast for this segment (US082); its wind overrides the DSL baseline.</li>
 *   <li>{@link #DSL_BASELINE} — no forecast attached (or it could not be loaded);
 *       the DSL's per-segment baseline wind is used as the fallback.</li>
 * </ul>
 */
public enum WindSource {
    ATTACHED_FORECAST,
    DSL_BASELINE
}