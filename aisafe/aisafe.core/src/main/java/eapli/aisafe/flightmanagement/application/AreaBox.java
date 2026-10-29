package eapli.aisafe.flightmanagement.application;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;

/**
 * The axis-aligned latitude/longitude bounding box of an air control area, plus
 * the "does this plan cross the area" test the FCO dashboard and (later) the area
 * simulation egress both need.
 *
 * <p>The air control area's {@link AirControlArea#boundary() boundary} is already
 * a lat/lon rectangle, so a candidate-level crossing check needs no metric
 * projection: a small line-vs-rectangle intersection test over the recompiled
 * plan segments is enough to answer "could this flight be part of my area's
 * simulation?". The full metric projection + boundary clip that builds the SCOMP
 * scenario is a separate, heavier concern; this box is the cheap shared predicate
 * so candidate lists and generated scenarios agree on which flights touch the
 * area.</p>
 *
 * <p>A segment counts as inside when either endpoint is inside the box or the
 * segment intersects the rectangle. This keeps candidate selection aligned with
 * the SCOMP scenario builder, which later clips the segment to the exact area
 * entry/exit points.</p>
 */
public record AreaBox(double minLat, double maxLat, double minLon, double maxLon) {

    private static final double EPS = 1e-9;

    public AreaBox {
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat must not exceed maxLat");
        }
        if (minLon > maxLon) {
            throw new IllegalArgumentException("minLon must not exceed maxLon");
        }
    }

    /** The bounding box of an air control area's geographic boundary. */
    public static AreaBox of(final AirControlArea area) {
        final var sw = area.boundary().southwest();
        final var ne = area.boundary().northeast();
        return new AreaBox(
                Math.min(sw.latitude(), ne.latitude()),
                Math.max(sw.latitude(), ne.latitude()),
                Math.min(sw.longitude(), ne.longitude()),
                Math.max(sw.longitude(), ne.longitude()));
    }

    /** Whether the given point falls within the box (edges inclusive). */
    public boolean contains(final double latitude, final double longitude) {
        return latitude >= minLat && latitude <= maxLat
                && longitude >= minLon && longitude <= maxLon;
    }

    /** Whether the segment touches or crosses the box. */
    public boolean touches(final ParsedSegment segment) {
        return contains(segment.startLatitude(), segment.startLongitude())
                || contains(segment.endLatitude(), segment.endLongitude())
                || intersects(segment);
    }

    /** How many of the plan's segments touch the area (0 means it never enters). */
    public int segmentsInside(final ParsedFlightPlan plan) {
        int count = 0;
        for (final ParsedSegment segment : plan.segments()) {
            if (touches(segment)) {
                count++;
            }
        }
        return count;
    }

    /** Whether the plan enters the area at all. */
    public boolean crossedBy(final ParsedFlightPlan plan) {
        return segmentsInside(plan) > 0;
    }

    private boolean intersects(final ParsedSegment segment) {
        final double x0 = segment.startLongitude();
        final double y0 = segment.startLatitude();
        final double x1 = segment.endLongitude();
        final double y1 = segment.endLatitude();
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        final double[] u = { 0.0, 1.0 };

        return clipBoundary(-dx, x0 - minLon, u)
                && clipBoundary(dx, maxLon - x0, u)
                && clipBoundary(-dy, y0 - minLat, u)
                && clipBoundary(dy, maxLat - y0, u)
                && u[0] <= u[1];
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
}
