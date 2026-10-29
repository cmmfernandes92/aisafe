package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Fuel;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

/**
 * §3.4.5 Leg-level rules:
 * <ul>
 *   <li>Each leg must contain exactly one departure.</li>
 *   <li>Each leg must contain exactly one arrival.</li>
 *   <li>Each leg must contain at least one segment.</li>
 *   <li>Fuel quantity must be strictly positive and associated with a specific leg.</li>
 * </ul>
 */
public final class LegLevelRule implements SemanticRule {

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        for (Flight f : file.flights()) {
            if (f == null) continue;

            if (f.legs() == null || f.legs().isEmpty()) {
                c.error(SemanticErrorCode.FLIGHT_MISSING_LEGS,
                        "Flight '" + safeId(f) + "' has no legs; at least one leg is required.",
                        safePos(f.position()));
                continue;
            }

            for (Leg leg : f.legs()) {
                if (leg == null) continue;
                String where = "flight '" + safeId(f) + "', leg " + leg.number();
                Position pos = safePos(leg.position());

                if (leg.departure() == null) {
                    c.error(SemanticErrorCode.LEG_MISSING_DEPARTURE,
                            "Missing departure in " + where + " (each leg requires exactly one departure).", pos);
                }
                if (leg.arrival() == null) {
                    c.error(SemanticErrorCode.LEG_MISSING_ARRIVAL,
                            "Missing arrival in " + where + " (each leg requires exactly one arrival).", pos);
                }
                if (leg.segments() == null || leg.segments().isEmpty()) {
                    c.error(SemanticErrorCode.LEG_MISSING_SEGMENTS,
                            "Missing segments in " + where + " (each leg requires at least one segment).", pos);
                }
                if (leg.fuel() == null) {
                    c.error(SemanticErrorCode.LEG_MISSING_FUEL,
                            "Missing fuel block in " + where + " (each leg requires exactly one fuel value).", pos);
                } else {
                    Fuel fuel = leg.fuel();
                    if (!(fuel.quantity() > 0)) {
                        c.error(SemanticErrorCode.FUEL_NOT_POSITIVE,
                                "Fuel quantity in " + where + " must be strictly positive (was "
                                        + fuel.quantity() + ").",
                                safePos(fuel.position()));
                    }
                }
            }
        }
    }

    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}