package eapli.aisafe.flightmanagement.application.areasimulation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.flightmanagement.application.AreaBox;
import eapli.aisafe.flightmanagement.application.AreaCandidateFlightsService.AreaCandidate;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.framework.validations.Preconditions;

public class AreaSimulationScenarioBuilder {

    public static final int MAX_FLIGHTS = 50;
    public static final int MAX_SEGMENTS = 8;
    public static final int HISTORY_SIZE = 600;

    private static final double SECONDS_PER_STEP = 60.0;
    private static final int HISTORY_DEPTH = 2;
    private static final int MAX_VIOLATIONS = 3;
    private static final double H_MIN = 8.0;
    private static final double V_MIN = 600.0;
    private static final double MIN_AREA_ALTITUDE = 0.0;
    private static final double DEFAULT_AREA_ALTITUDE_MAX = 20000.0;
    private static final double KNOTS_TO_MPS = 0.514444;
    private static final double EPS = 1e-9;
    private static final DateTimeFormatter SCENARIO_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public AreaSimulationScenario build(final AirControlArea area,
            final List<AreaCandidate> candidates,
            final SimulationWindow window,
            final SurrogateIdMap idMap) {
        Preconditions.noneNull(area, candidates, window, idMap);

        final AreaBox box = AreaBox.of(area);
        final double refLat = (box.minLat() + box.maxLat()) / 2.0;
        final double refLon = (box.minLon() + box.maxLon()) / 2.0;
        final EquirectangularProjection projection = new EquirectangularProjection(refLat, refLon);
        final ProjectedBounds bounds = projectedBounds(box, projection);

        final List<ScenarioFlight> flights = new ArrayList<>();
        double maxSegmentAltitude = MIN_AREA_ALTITUDE;
        double longestFlightSeconds = SECONDS_PER_STEP;

        final List<AreaCandidate> sorted = candidates.stream()
                .sorted(Comparator.comparing(candidate -> candidate.flight().departure()))
                .toList();
        // Staggered entry (SCOMP option (a)): the first admitted flight enters at
        // step 0; later flights enter at the step matching their departure offset,
        // so flights scheduled apart are not checked against each other at step 0.
        LocalDateTime entryAnchor = null;
        for (final AreaCandidate candidate : sorted) {
            if (flights.size() >= MAX_FLIGHTS) {
                break;
            }

            final List<ScenarioSegment> segments = clippedSegments(candidate.plan(), box);
            if (segments.isEmpty()) {
                continue;
            }

            final LocalDateTime departure = candidate.flight().departure();
            if (entryAnchor == null) {
                entryAnchor = departure;
            }

            for (final ScenarioSegment segment : segments) {
                maxSegmentAltitude = Math.max(maxSegmentAltitude,
                        Math.max(segment.startAltitudeMeters(), segment.endAltitudeMeters()));
            }
            longestFlightSeconds = Math.max(longestFlightSeconds, estimatedSeconds(segments));

            flights.add(new ScenarioFlight(
                    idMap.idFor(candidate.flight().identity()),
                    FlightProfile.defaultJet(),
                    segments,
                    entryStep(entryAnchor, departure)));
        }

        if (flights.isEmpty()) {
            throw new IllegalArgumentException("No candidate flight has route geometry inside the area");
        }

        final SimParams params = new SimParams(
                scenarioName(area, window),
                totalSteps(window, longestFlightSeconds),
                SECONDS_PER_STEP,
                HISTORY_DEPTH,
                MAX_VIOLATIONS,
                H_MIN,
                V_MIN,
                bounds.xMin(),
                bounds.xMax(),
                bounds.yMin(),
                bounds.yMax(),
                MIN_AREA_ALTITUDE,
                Math.max(DEFAULT_AREA_ALTITUDE_MAX, maxSegmentAltitude),
                refLat,
                refLon);

        return new AreaSimulationScenario(params, flights, idMap.asMap());
    }

    private static List<ScenarioSegment> clippedSegments(final ParsedFlightPlan plan, final AreaBox box) {
        final List<ScenarioSegment> segments = new ArrayList<>();
        for (final ParsedSegment segment : plan.segments()) {
            if (segments.size() >= MAX_SEGMENTS) {
                break;
            }
            final ClippedSegment clipped = clipToBox(segment, box);
            if (clipped == null) {
                continue;
            }
            segments.add(new ScenarioSegment(
                    mode(segment.altitudeMeters(), segment.altitudeMeters()),
                    clipped.startLat(),
                    clipped.startLon(),
                    segment.altitudeMeters(),
                    clipped.endLat(),
                    clipped.endLon(),
                    segment.altitudeMeters()));
        }
        return segments;
    }

    private static ClippedSegment clipToBox(final ParsedSegment segment, final AreaBox box) {
        final double x0 = segment.startLongitude();
        final double y0 = segment.startLatitude();
        final double x1 = segment.endLongitude();
        final double y1 = segment.endLatitude();
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        final double[] u = { 0.0, 1.0 };

        if (!clipBoundary(-dx, x0 - box.minLon(), u)
                || !clipBoundary(dx, box.maxLon() - x0, u)
                || !clipBoundary(-dy, y0 - box.minLat(), u)
                || !clipBoundary(dy, box.maxLat() - y0, u)) {
            return null;
        }

        final double u0 = clamp01(u[0]);
        final double u1 = clamp01(u[1]);
        final double startLat = interpolate(y0, y1, u0);
        final double startLon = interpolate(x0, x1, u0);
        final double endLat = interpolate(y0, y1, u1);
        final double endLon = interpolate(x0, x1, u1);

        if (Math.abs(startLat - endLat) < EPS && Math.abs(startLon - endLon) < EPS) {
            return null;
        }
        return new ClippedSegment(startLat, startLon, endLat, endLon);
    }

    private static boolean clipBoundary(final double p, final double q, final double[] u) {
        if (Math.abs(p) < EPS) {
            return q >= -EPS;
        }

        final double r = q / p;
        if (p < 0.0) {
            if (r > u[1]) {
                return false;
            }
            if (r > u[0]) {
                u[0] = r;
            }
        } else {
            if (r < u[0]) {
                return false;
            }
            if (r < u[1]) {
                u[1] = r;
            }
        }
        return true;
    }

    private static double interpolate(final double from, final double to, final double fraction) {
        return from + (to - from) * fraction;
    }

    private static double clamp01(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String mode(final double startAltitude, final double endAltitude) {
        if (endAltitude > startAltitude + 1.0) {
            return "climb";
        }
        if (endAltitude < startAltitude - 1.0) {
            return "descend";
        }
        return "cruise";
    }

    private static ProjectedBounds projectedBounds(final AreaBox box,
            final EquirectangularProjection projection) {
        final List<EquirectangularProjection.Point> points = List.of(
                projection.project(box.minLat(), box.minLon()),
                projection.project(box.minLat(), box.maxLon()),
                projection.project(box.maxLat(), box.minLon()),
                projection.project(box.maxLat(), box.maxLon()));

        double xMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        for (final EquirectangularProjection.Point point : points) {
            xMin = Math.min(xMin, point.x());
            xMax = Math.max(xMax, point.x());
            yMin = Math.min(yMin, point.y());
            yMax = Math.max(yMax, point.y());
        }
        return new ProjectedBounds(xMin, xMax, yMin, yMax);
    }

    /**
     * The step a flight enters, relative to the first admitted flight (which enters
     * at 0). Capped at {@code HISTORY_SIZE - 1} so a far-future departure still maps
     * to a valid step; if it lands beyond the run's {@code total_steps} the engine
     * simply never activates that flight (no violation), which is the correct no-op.
     */
    private static int entryStep(final LocalDateTime anchor, final LocalDateTime departure) {
        final long seconds = Math.max(0L, Duration.between(anchor, departure).getSeconds());
        final long step = Math.round(seconds / SECONDS_PER_STEP);
        return (int) Math.min(step, (long) (HISTORY_SIZE - 1));
    }

    private static int totalSteps(final SimulationWindow window, final double longestFlightSeconds) {
        final long windowSeconds = Math.max(1L, Duration.between(window.from(), window.to()).getSeconds());
        final double targetSeconds = Math.max(windowSeconds, longestFlightSeconds);
        return Math.max(1, Math.min(HISTORY_SIZE, (int) Math.ceil(targetSeconds / SECONDS_PER_STEP)));
    }

    private static double estimatedSeconds(final List<ScenarioSegment> segments) {
        double metres = 0.0;
        for (final ScenarioSegment segment : segments) {
            metres += EquirectangularProjection.haversineMeters(
                    segment.startLatitude(), segment.startLongitude(),
                    segment.endLatitude(), segment.endLongitude());
        }
        return metres / (FlightProfile.defaultJet().cruiseSpeedKnots() * KNOTS_TO_MPS);
    }

    private static String scenarioName(final AirControlArea area, final SimulationWindow window) {
        final String name = area.code() + "-" + SCENARIO_STAMP.format(window.from());
        return name.length() <= 31 ? name : name.substring(0, 31);
    }

    private record ClippedSegment(double startLat, double startLon, double endLat, double endLon) {
    }

    private record ProjectedBounds(double xMin, double xMax, double yMin, double yMax) {
    }
}
