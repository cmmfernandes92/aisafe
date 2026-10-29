package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Arrival;
import lapr4.lprog.flightdsl.ast.Departure;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Length;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.ast.Segment;
import lapr4.lprog.flightdsl.ast.Slot;
import lapr4.lprog.flightdsl.ast.TemporalLiteral;
import lapr4.lprog.flightdsl.ast.Wind;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;
import lapr4.lprog.flightdsl.semantic.SemanticErrorCode;

import java.util.regex.Pattern;

/**
 * §3.4.5 Domain consistency:
 * <ul>
 *   <li>Airport codes must respect the defined format (IATA 3 letters or ICAO 4 letters).</li>
 *   <li>Numeric values (altitudes, widths, wind speed and fuel) must be positive.</li>
 *   <li>Date and time values must represent valid calendar values.</li>
 * </ul>
 *
 * <p>Fuel positivity is enforced by {@link LegLevelRule} (under code {@code FUEL_NOT_POSITIVE}),
 * so this rule does not re-check fuel to avoid duplicate findings.
 */
public final class DomainConsistencyRule implements SemanticRule {

    private static final Pattern IATA = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern ICAO = Pattern.compile("^[A-Z]{4}$");

    @Override
    public void check(FlightPlanFile file, ErrorCollector c) {
        if (file == null || file.flights() == null) return;
        for (Flight f : file.flights()) {
            if (f == null) continue;
            checkRouteAirports(f, c);
            if (f.legs() == null) continue;
            for (Leg leg : f.legs()) {
                if (leg == null) continue;
                checkDeparture(f, leg, c);
                checkArrival(f, leg, c);
                checkSegments(f, leg, c);
            }
        }
    }

    private void checkRouteAirports(Flight f, ErrorCollector c) {
        Route route = f.route();
        if (route == null) return;
        checkAirportCode(f, route.origin(), "route origin", c);
        checkAirportCode(f, route.destination(), "route destination", c);
    }

    private void checkDeparture(Flight f, Leg leg, ErrorCollector c) {
        Departure d = leg.departure();
        if (d == null) return;
        checkAirportCode(f, d.airport(), "leg " + leg.number() + " departure", c);
        checkDateTime(f, d.when(), "leg " + leg.number() + " departure", c);
    }

    private void checkArrival(Flight f, Leg leg, ErrorCollector c) {
        Arrival a = leg.arrival();
        if (a == null) return;
        checkAirportCode(f, a.airport(), "leg " + leg.number() + " arrival", c);
        checkDateTime(f, a.when(), "leg " + leg.number() + " arrival", c);
    }

    private void checkSegments(Flight f, Leg leg, ErrorCollector c) {
        if (leg.segments() == null) return;
        int idx = 0;
        for (Segment seg : leg.segments()) {
            idx++;
            if (seg == null) continue;
            if (seg.slots() != null) {
                int slotIdx = 0;
                for (Slot slot : seg.slots()) {
                    slotIdx++;
                    if (slot == null) continue;
                    String wherePrefix = "flight '" + safeId(f) + "', leg " + leg.number()
                            + ", segment #" + idx + ", slot #" + slotIdx;
                    checkPositive(slot.altitude(), wherePrefix + " altitude", c);
                    checkPositive(slot.width(), wherePrefix + " width", c);
                }
            }
            Wind w = seg.wind();
            if (w != null && !(w.speed() > 0)) {
                c.error(SemanticErrorCode.NUMERIC_NOT_POSITIVE,
                        "flight '" + safeId(f) + "', leg " + leg.number()
                                + ", segment #" + idx + " wind speed must be positive (was " + w.speed() + " m/s).",
                        safePos(w.position()));
            }
        }
    }

    private void checkPositive(Length len, String label, ErrorCollector c) {
        if (len == null) return;
        if (!(len.value() > 0)) {
            c.error(SemanticErrorCode.NUMERIC_NOT_POSITIVE,
                    label + " must be positive (was " + len.value() + " m).",
                    safePos(len.position()));
        }
    }

    private void checkAirportCode(Flight f, AirportRef ref, String label, ErrorCollector c) {
        if (ref == null || ref.code() == null) return;
        String code = ref.code();
        if (!IATA.matcher(code).matches() && !ICAO.matcher(code).matches()) {
            c.error(SemanticErrorCode.AIRPORT_CODE_BAD_FORMAT,
                    "Flight '" + safeId(f) + "': " + label + " code '" + code
                            + "' is not a valid IATA (3 letters) or ICAO (4 letters) code.",
                    safePos(ref.position()));
        }
    }

    private void checkDateTime(Flight f, TemporalLiteral t, String label, ErrorCollector c) {
        if (t == null) return;
        if (!t.isValidCalendar()) {
            c.error(SemanticErrorCode.DATETIME_INVALID_CALENDAR,
                    "Flight '" + safeId(f) + "': " + label + " datetime '" + t.raw()
                            + "' is not a valid calendar value.",
                    safePos(t.position()));
        }
    }

    private static Position safePos(Position p) { return p == null ? Position.UNKNOWN : p; }
    private static String safeId(Flight f) { return f.identifier() == null ? "<missing>" : f.identifier(); }
}