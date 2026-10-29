package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * §3.4.5 Route coherence:
 * <ul>
 *   <li>Route origin must match the first leg's departure airport.</li>
 *   <li>Route destination must match the last leg's arrival airport.</li>
 *   <li>In a route, the same airport cannot be visited twice — interpreted here as:
 *       the sequence (first leg's departure, then every leg's arrival) must not repeat.</li>
 * </ul>
 */
public final class RouteCoherenceRule implements SemanticRule {

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        for (Flight f : file.flights()) {
            if (f == null) continue;
            Route route = f.route();
            List<Leg> legs = f.legs();
            if (route == null || legs == null || legs.isEmpty()) continue;

            checkOrigin(f, route, legs, c);
            checkDestination(f, route, legs, c);
            checkNoRepeatedAirports(f, legs, c);
        }
    }

    private void checkOrigin(Flight f, Route route, List<Leg> legs, ErrorCollector c) {
        AirportRef firstDep = legs.get(0) != null && legs.get(0).departure() != null
                ? legs.get(0).departure().airport() : null;
        AirportRef origin = route.origin();
        if (origin == null || firstDep == null) return;
        if (!eq(origin.code(), firstDep.code())) {
            c.error(SemanticErrorCode.ROUTE_ORIGIN_MISMATCH,
                    "Flight '" + safeId(f) + "': route origin '" + origin.code()
                            + "' does not match first leg's departure '" + firstDep.code() + "'.",
                    safePos(origin.position()));
        }
    }

    private void checkDestination(Flight f, Route route, List<Leg> legs, ErrorCollector c) {
        Leg last = legs.get(legs.size() - 1);
        AirportRef lastArr = last != null && last.arrival() != null ? last.arrival().airport() : null;
        AirportRef destination = route.destination();
        if (destination == null || lastArr == null) return;
        if (!eq(destination.code(), lastArr.code())) {
            c.error(SemanticErrorCode.ROUTE_DESTINATION_MISMATCH,
                    "Flight '" + safeId(f) + "': route destination '" + destination.code()
                            + "' does not match last leg's arrival '" + lastArr.code() + "'.",
                    safePos(destination.position()));
        }
    }

    private void checkNoRepeatedAirports(Flight f, List<Leg> legs, ErrorCollector c) {
        List<AirportRef> sequence = new ArrayList<>();
        Leg first = legs.get(0);
        if (first != null && first.departure() != null && first.departure().airport() != null) {
            sequence.add(first.departure().airport());
        }
        for (Leg leg : legs) {
            if (leg != null && leg.arrival() != null && leg.arrival().airport() != null) {
                sequence.add(leg.arrival().airport());
            }
        }

        Set<String> seen = new HashSet<>();
        for (AirportRef a : sequence) {
            // sequence only contains non-null refs by construction above; only the code
            // can legitimately be null, so only that needs to be guarded here.
            if (a.code() == null) continue;
            if (!seen.add(a.code())) {
                c.error(SemanticErrorCode.ROUTE_AIRPORT_REPEATED,
                        "Flight '" + safeId(f) + "': airport '" + a.code()
                                + "' is visited more than once in the route.",
                        safePos(a.position()));
            }
        }
    }

    private static boolean eq(String a, String b) { return a != null && a.equals(b); }
    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}