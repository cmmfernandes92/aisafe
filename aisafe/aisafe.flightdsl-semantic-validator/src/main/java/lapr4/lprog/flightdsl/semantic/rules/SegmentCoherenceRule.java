package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Segment;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

/**
 * §3.4.5 Segment coherence:
 * <ul>
 *   <li>Segment start and end coordinates must be different.</li>
 *   <li>Segments must belong to exactly one leg (structurally enforced by the grammar).</li>
 * </ul>
 */
public final class SegmentCoherenceRule implements SemanticRule {

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        for (Flight f : file.flights()) {
            if (f == null || f.legs() == null) continue;
            for (Leg leg : f.legs()) {
                if (leg == null || leg.segments() == null) continue;
                int segIndex = 0;
                for (Segment seg : leg.segments()) {
                    segIndex++;
                    if (seg == null || seg.start() == null || seg.end() == null) continue;
                    if (seg.start().samePointAs(seg.end())) {
                        String where = "flight '" + safeId(f) + "', leg " + leg.number() + ", segment #" + segIndex;
                        c.error(SemanticErrorCode.SEGMENT_START_EQUALS_END,
                                "Start and end coordinates are equal in " + where
                                        + " (" + seg.start().latitude() + ", " + seg.start().longitude() + ").",
                                safePos(seg.position()));
                    }
                }
            }
        }
    }

    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}