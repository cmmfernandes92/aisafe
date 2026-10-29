package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Arrival;
import lapr4.lprog.flightdsl.ast.Coordinate;
import lapr4.lprog.flightdsl.ast.Departure;
import lapr4.lprog.flightdsl.ast.Flight;
import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.ast.FlightType;
import lapr4.lprog.flightdsl.ast.Fuel;
import lapr4.lprog.flightdsl.ast.FuelUnit;
import lapr4.lprog.flightdsl.ast.Leg;
import lapr4.lprog.flightdsl.ast.Length;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.Route;
import lapr4.lprog.flightdsl.ast.Segment;
import lapr4.lprog.flightdsl.ast.Slot;
import lapr4.lprog.flightdsl.ast.TemporalLiteral;
import lapr4.lprog.flightdsl.ast.Wind;

import java.util.List;

/**
 * Tiny AST builder used by unit tests so each test reads as a story
 * (declare a flight plan → mutate one field → assert one finding).
 */
final class AstFixtures {

    private AstFixtures() { }

    static Position pos(int line) { return new Position(line, 1); }

    static AirportRef airport(String code) { return new AirportRef(code, Position.UNKNOWN); }

    static Coordinate coord(double lat, double lon) { return new Coordinate(lat, lon, Position.UNKNOWN); }

    static TemporalLiteral dt(String raw) { return TemporalLiteral.of(raw, Position.UNKNOWN); }

    static Slot slot(double altitudeM, double widthM) {
        return new Slot(
                new Length(altitudeM, Position.UNKNOWN),
                new Length(widthM, Position.UNKNOWN),
                Position.UNKNOWN);
    }

    static Wind wind(double dir, double speed) { return new Wind(dir, speed, Position.UNKNOWN); }

    static Segment segment(Coordinate start, Coordinate end) {
        return new Segment(start, end, List.of(slot(10000, 500)), wind(180, 5), Position.UNKNOWN);
    }

    static Fuel fuel(double quantity) { return new Fuel(quantity, FuelUnit.KG, Position.UNKNOWN); }

    static Departure departure(String airportCode, String when) {
        return new Departure(airport(airportCode), dt(when), Position.UNKNOWN);
    }

    static Arrival arrival(String airportCode, String when) {
        return new Arrival(airport(airportCode), dt(when), Position.UNKNOWN);
    }

    /** A leg with one segment from (0,0) to (1,1), realistic-ish defaults. */
    static Leg simpleLeg(int number, String depCode, String depWhen, String arrCode, String arrWhen) {
        return new Leg(
                number,
                departure(depCode, depWhen),
                arrival(arrCode, arrWhen),
                fuel(5000),
                List.of(segment(coord(41.2, -8.7), coord(38.7, -9.1))),
                Position.UNKNOWN);
    }

    /** A valid single-leg flight OPO -> LIS. */
    static Flight validSingleLegFlight(String id) {
        return new Flight(
                id,
                FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(simpleLeg(1, "OPO", "2026-06-01T08:00", "LIS", "2026-06-01T09:00")),
                Position.UNKNOWN);
    }

    /** A valid two-leg flight OPO -> MAD -> LIS. */
    static Flight validTwoLegFlight(String id) {
        return new Flight(
                id,
                FlightType.REGULAR,
                new Route(airport("OPO"), airport("LIS"), Position.UNKNOWN),
                List.of(
                        simpleLeg(1, "OPO", "2026-06-01T08:00", "MAD", "2026-06-01T10:00"),
                        simpleLeg(2, "MAD", "2026-06-01T11:30", "LIS", "2026-06-01T12:30")),
                Position.UNKNOWN);
    }

    static FlightPlanFile fileOf(Flight... flights) {
        return new FlightPlanFile(List.of(flights), Position.UNKNOWN);
    }
}