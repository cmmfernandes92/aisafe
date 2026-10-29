package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * §3.4.5 Flight-level rules:
 * <ul>
 *   <li>The flight identifier must be unique within the file.</li>
 *   <li>The flight type must be one of the allowed values.</li>
 * </ul>
 */
public final class FlightLevelRule implements SemanticRule {

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        List<Flight> flights = file.flights();

        // Identifier uniqueness — flag every duplicate occurrence.
        Map<String, List<Position>> byId = new HashMap<>();
        for (Flight f : flights) {
            if (f == null || f.identifier() == null) continue;
            byId.computeIfAbsent(f.identifier(), k -> new ArrayList<>()).add(safePos(f.position()));
        }
        for (Map.Entry<String, List<Position>> e : byId.entrySet()) {
            if (e.getValue().size() > 1) {
                for (Position p : e.getValue()) {
                    c.error(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE,
                            "Flight identifier '" + e.getKey() + "' is declared "
                                    + e.getValue().size() + " times in this file; identifiers must be unique.",
                            p);
                }
            }
        }

        // Flight type validity.
        for (Flight f : flights) {
            if (f == null) continue;
            if (f.type() == null) {
                c.error(SemanticErrorCode.FLIGHT_TYPE_INVALID,
                        "Flight '" + safeId(f) + "' has no flight type (allowed: REGULAR, CHARTER).",
                        safePos(f.position()));
            }
        }
    }

    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}