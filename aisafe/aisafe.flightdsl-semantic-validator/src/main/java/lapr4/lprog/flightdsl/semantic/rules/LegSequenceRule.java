package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.Arrival;
import lapr4.lprog.flightdsl.ast.Departure;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

import java.util.List;

/**
 * §3.4.5 Leg sequence coherence:
 * <ul>
 *   <li>The arrival airport of leg <em>n</em> must match the departure airport of leg <em>n+1</em>.</li>
 *   <li>The arrival time of leg <em>n</em> must precede the departure time of leg <em>n+1</em>.</li>
 * </ul>
 */
public final class LegSequenceRule implements SemanticRule {

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        for (Flight f : file.flights()) {
            if (f == null || f.legs() == null) continue;
            List<Leg> legs = f.legs();
            for (int i = 0; i + 1 < legs.size(); i++) {
                Leg current = legs.get(i);
                Leg next = legs.get(i + 1);
                if (current == null || next == null) continue;

                checkAirportContinuity(f, current, next, c);
                checkTimeOrdering(f, current, next, c);
            }
        }
    }

    private void checkAirportContinuity(Flight f, Leg current, Leg next, ErrorCollector c) {
        Arrival arr = current.arrival();
        Departure dep = next.departure();
        if (arr == null || dep == null || arr.airport() == null || dep.airport() == null) return;
        String arrCode = arr.airport().code();
        String depCode = dep.airport().code();
        if (arrCode == null || depCode == null) return;
        if (!arrCode.equals(depCode)) {
            c.error(SemanticErrorCode.LEG_SEQUENCE_AIRPORT_MISMATCH,
                    "Flight '" + safeId(f) + "': arrival of leg " + current.number()
                            + " is '" + arrCode + "' but departure of leg " + next.number()
                            + " is '" + depCode + "' — they must match.",
                    safePos(dep.position()));
        }
    }

    private void checkTimeOrdering(Flight f, Leg current, Leg next, ErrorCollector c) {
        Arrival arr = current.arrival();
        Departure dep = next.departure();
        if (arr == null || dep == null || arr.when() == null || dep.when() == null) return;
        if (arr.when().value() == null || dep.when().value() == null) return; // skip if calendar-invalid
        if (!arr.when().value().isBefore(dep.when().value())) {
            c.error(SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED,
                    "Flight '" + safeId(f) + "': arrival of leg " + current.number()
                            + " (" + arr.when().raw() + ") must precede departure of leg "
                            + next.number() + " (" + dep.when().raw() + ").",
                    safePos(dep.position()));
        }
    }

    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}