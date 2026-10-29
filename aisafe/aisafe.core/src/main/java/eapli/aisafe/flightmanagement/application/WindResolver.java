package eapli.aisafe.flightmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightmanagement.domain.SegmentKey;
import eapli.aisafe.flightmanagement.domain.physics.ResolvedSegment;
import eapli.aisafe.flightmanagement.domain.physics.WindSource;
import eapli.aisafe.weather.domain.WeatherData;
import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.aisafe.weather.repositories.WeatherDataRepository;

/**
 * Resolves the effective per-segment wind for US085: a <em>left join</em> from
 * each DSL segment to the pilot's attached forecast (US082), with the DSL's
 * baseline wind as the fallback (design-notes §3.2).
 *
 * <p>For each segment, keyed by the shared {@link SegmentKey#of(int, int)}
 * convention {@code (legNumber, segmentIndex)}: if the pilot attached a
 * {@link WeatherDataId} and it still loads, that forecast's wind overrides the
 * baseline; if nothing is attached <em>or the id is dangling</em> (the weather was
 * deleted after attaching), the DSL baseline wind is used and the resolver never
 * throws. No altitude re-matching — the pilot already matched altitude/provider
 * when choosing the forecast.</p>
 *
 * <p><b>Unit note:</b> the DSL baseline wind is m/s; persisted
 * {@code WeatherWindSpeed} is <em>knots</em> (the bulk import feeds knots and the
 * VO renders {@code " kt"}), so an attached forecast's speed is normalised
 * knots→m/s here before it enters the m/s physics. Direction is degrees in both.</p>
 */
public class WindResolver {

    /** Knots → m/s (1 kt = 0.514444 m/s). Persisted weather wind speed is knots. */
    public static final double KNOTS_TO_MPS = 0.514444;

    private final WeatherDataRepository weatherRepository;

    public WindResolver(final WeatherDataRepository weatherRepository) {
        if (weatherRepository == null) {
            throw new IllegalArgumentException("Weather repository must not be null");
        }
        this.weatherRepository = weatherRepository;
    }

    /**
     * Resolves every segment of the plan, in route order.
     *
     * @param plan        the recompiled, DSL-free flight plan (geometry + baseline wind)
     * @param assignments the pilot's per-segment weather choices (US082), possibly empty
     * @return one {@link ResolvedSegment} per input segment, in the same order
     */
    public List<ResolvedSegment> resolve(final ParsedFlightPlan plan,
            final Map<SegmentKey, WeatherDataId> assignments) {
        final List<ResolvedSegment> resolved = new ArrayList<>(plan.segments().size());
        for (final ParsedSegment segment : plan.segments()) {
            resolved.add(resolveOne(segment, assignments));
        }
        return resolved;
    }

    private ResolvedSegment resolveOne(final ParsedSegment segment,
            final Map<SegmentKey, WeatherDataId> assignments) {
        final SegmentKey key = SegmentKey.of(segment.legNumber(), segment.segmentIndex());
        final WeatherDataId attachedId = assignments == null ? null : assignments.get(key);

        if (attachedId != null) {
            final WeatherData forecast = weatherRepository.ofIdentity(attachedId).orElse(null);
            if (forecast != null) {
                return new ResolvedSegment(
                        segment.legNumber(), segment.segmentIndex(),
                        segment.startLatitude(), segment.startLongitude(),
                        segment.endLatitude(), segment.endLongitude(),
                        segment.altitudeMeters(),
                        forecast.windDirection().value(),
                        forecast.windSpeed().value() * KNOTS_TO_MPS,
                        WindSource.ATTACHED_FORECAST);
            }
            // attached but dangling (deleted after attach) → fall back to DSL, never crash
        }

        return new ResolvedSegment(
                segment.legNumber(), segment.segmentIndex(),
                segment.startLatitude(), segment.startLongitude(),
                segment.endLatitude(), segment.endLongitude(),
                segment.altitudeMeters(),
                segment.baselineWindDirectionDeg(),
                segment.baselineWindSpeedMps(),
                WindSource.DSL_BASELINE);
    }
}