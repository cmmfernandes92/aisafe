package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.AirportRef;
import lapr4.lprog.flightdsl.ast.Arrival;
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
import lapr4.lprog.flightdsl.semantic.rules.DomainConsistencyRule;
import lapr4.lprog.flightdsl.semantic.rules.FlightLevelRule;
import lapr4.lprog.flightdsl.semantic.rules.LegLevelRule;
import lapr4.lprog.flightdsl.semantic.rules.LegSequenceRule;
import lapr4.lprog.flightdsl.semantic.rules.RouteCoherenceRule;
import lapr4.lprog.flightdsl.semantic.rules.SegmentCoherenceRule;
import lapr4.lprog.flightdsl.semantic.rules.SemanticRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the defensive null guards present in every {@link SemanticRule} implementation
 * so the rule-pattern stays robust even when a previous parsing phase produces a partial AST.
 */
@DisplayName("Null safety — every rule must short-circuit on null input")
class RuleNullSafetyTest {

    private static final List<SemanticRule> RULES = List.of(
            new FlightLevelRule(),
            new LegLevelRule(),
            new LegSequenceRule(),
            new RouteCoherenceRule(),
            new SegmentCoherenceRule(),
            new DomainConsistencyRule());

    private static FlightPlanFile fileOfNullableFlights(Flight... flights) {
        final List<Flight> list = new ArrayList<>();
        list.addAll(Arrays.asList(flights));
        return new FlightPlanFile(list, Position.UNKNOWN);
    }

    private static Flight flightWith(String id, FlightType type, Route route, List<Leg> legs) {
        return new Flight(id, type, route, legs, Position.UNKNOWN);
    }

    private static Route route(String origin, String destination) {
        return new Route(
                origin == null ? null : new AirportRef(origin, Position.UNKNOWN),
                destination == null ? null : new AirportRef(destination, Position.UNKNOWN),
                Position.UNKNOWN);
    }

    private static Leg leg(int n, Departure d, Arrival a, Fuel fuel, List<Segment> segs) {
        return new Leg(n, d, a, fuel, segs, Position.UNKNOWN);
    }

    private static Departure dep(String code, String when) {
        return new Departure(
                code == null ? null : new AirportRef(code, Position.UNKNOWN),
                when == null ? null : TemporalLiteral.of(when, Position.UNKNOWN),
                Position.UNKNOWN);
    }

    private static Arrival arr(String code, String when) {
        return new Arrival(
                code == null ? null : new AirportRef(code, Position.UNKNOWN),
                when == null ? null : TemporalLiteral.of(when, Position.UNKNOWN),
                Position.UNKNOWN);
    }

    // ───────────────────────────────────────────────────────────────────────
    // null file / null flights list
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All rules short-circuit on a null FlightPlanFile")
    void allRulesAcceptNullFile() {
        for (SemanticRule rule : RULES) {
            ErrorCollector c = new ErrorCollector();
            rule.check(null, c);
            assertTrue(c.errors().isEmpty(),
                    () -> rule.getClass().getSimpleName() + " produced errors on null file: " + c.errors());
        }
    }

    @Test
    @DisplayName("All rules short-circuit when FlightPlanFile.flights() is null")
    void allRulesAcceptNullFlightsList() {
        FlightPlanFile file = new FlightPlanFile(null, Position.UNKNOWN);
        for (SemanticRule rule : RULES) {
            ErrorCollector c = new ErrorCollector();
            rule.check(file, c);
            assertTrue(c.errors().isEmpty(),
                    () -> rule.getClass().getSimpleName() + " produced errors on null flights: " + c.errors());
        }
    }

    @Test
    @DisplayName("All rules skip null Flight entries in the list")
    void allRulesSkipNullFlights() {
        FlightPlanFile file = fileOfNullableFlights((Flight) null);
        for (SemanticRule rule : RULES) {
            ErrorCollector c = new ErrorCollector();
            rule.check(file, c);
            assertTrue(c.errors().isEmpty(),
                    () -> rule.getClass().getSimpleName() + " produced errors on null flight: " + c.errors());
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // null legs list / null leg entries
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LegLevelRule flags missing legs only when list is null too")
    void legLevelFlagsNullLegsList() {
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), null);
        ErrorCollector c = new ErrorCollector();
        new LegLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.FLIGHT_MISSING_LEGS));
    }

    @Test
    @DisplayName("Rules that iterate legs skip a null Leg entry without crashing")
    void rulesSkipNullLegEntry() {
        final List<Leg> legs = new ArrayList<>();
        legs.add(null);
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), legs);
        for (SemanticRule rule : RULES) {
            ErrorCollector c = new ErrorCollector();
            rule.check(fileOfNullableFlights(f), c);
            // No assertion on count — only that nothing throws.
        }
    }

    @Test
    @DisplayName("LegSequenceRule skips pair where current or next is null")
    void legSequenceSkipsNullEntries() {
        final List<Leg> legs = new ArrayList<>();
        legs.add(null);
        legs.add(null);
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), legs);
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    // ───────────────────────────────────────────────────────────────────────
    // LegLevelRule — branches for missing pieces and null identifier
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LegLevelRule labels flights without identifier as <missing>")
    void legLevelHandlesNullFlightIdentifier() {
        Leg badLeg = leg(1, null, arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(new Segment(
                        new lapr4.lprog.flightdsl.ast.Coordinate(0, 0, Position.UNKNOWN),
                        new lapr4.lprog.flightdsl.ast.Coordinate(1, 1, Position.UNKNOWN),
                        List.of(new Slot(new Length(10000, Position.UNKNOWN),
                                new Length(500, Position.UNKNOWN), Position.UNKNOWN)),
                        new Wind(180, 5, Position.UNKNOWN),
                        Position.UNKNOWN)));
        Flight f = flightWith(null, FlightType.REGULAR, route("OPO", "LIS"), List.of(badLeg));

        ErrorCollector c = new ErrorCollector();
        new LegLevelRule().check(fileOfNullableFlights(f), c);

        assertTrue(c.errors().stream()
                .anyMatch(e -> e.message().contains("<missing>")));
    }

    // ───────────────────────────────────────────────────────────────────────
    // SegmentCoherenceRule — null segments / null start/end
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SegmentCoherenceRule skips null segments and null start/end")
    void segmentCoherenceSkipsNullStartEnd() {
        final List<Segment> segs = new ArrayList<>();
        segs.add(null);
        segs.add(new Segment(null, null, List.of(), null, Position.UNKNOWN));
        Leg badLeg = leg(1, dep("OPO", "2026-06-01T08:00"), arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), segs);
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(badLeg));

        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.SEGMENT_START_EQUALS_END));
    }

    @Test
    @DisplayName("SegmentCoherenceRule labels segment in a flight without identifier as <missing>")
    void segmentCoherenceHandlesNullFlightId() {
        Segment same = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                List.of(), null, Position.UNKNOWN);
        Leg badLeg = leg(1, dep("OPO", "2026-06-01T08:00"), arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of(same));
        Flight f = flightWith(null, FlightType.REGULAR, route("OPO", "LIS"), List.of(badLeg));

        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.SEGMENT_START_EQUALS_END));
        assertTrue(c.errors().stream().anyMatch(e -> e.message().contains("<missing>")));
    }

    // ───────────────────────────────────────────────────────────────────────
    // DomainConsistencyRule — all internal null guards
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DomainConsistencyRule skips legs with null departure / arrival / segments / route")
    void domainConsistencySkipsAllInternalNulls() {
        Leg legWithNulls = leg(1, null, null, null, null);
        Flight f = flightWith("TP1", FlightType.REGULAR, null, List.of(legWithNulls));

        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("DomainConsistencyRule skips null segment, null slot, null wind, null airport-ref")
    void domainConsistencySkipsNullPieces() {
        final List<Slot> slots = new ArrayList<>();
        slots.add(null);
        slots.add(new Slot(null, null, Position.UNKNOWN));
        Segment segWithNullPieces = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(3, 4, Position.UNKNOWN),
                slots, null, Position.UNKNOWN);
        final List<Segment> segs = new ArrayList<>();
        segs.add(null);
        segs.add(segWithNullPieces);
        Leg leg = leg(1,
                new Departure(null, null, Position.UNKNOWN),
                new Arrival(null, null, Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                segs);
        Flight f = flightWith("TP1", FlightType.REGULAR, route(null, null), List.of(leg));

        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("DomainConsistencyRule flags wind speed ≤ 0")
    void domainConsistencyFlagsNonPositiveWind() {
        Segment seg = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(3, 4, Position.UNKNOWN),
                List.of(),
                new Wind(180, -1, Position.UNKNOWN),
                Position.UNKNOWN);
        Leg leg = leg(1,
                dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of(seg));
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(leg));

        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.NUMERIC_NOT_POSITIVE
                        && e.message().contains("wind speed")));
    }

    @Test
    @DisplayName("DomainConsistencyRule flags slot altitude ≤ 0 and width ≤ 0")
    void domainConsistencyFlagsNonPositiveSlotDimensions() {
        Segment seg = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(3, 4, Position.UNKNOWN),
                List.of(new Slot(
                        new Length(-1, Position.UNKNOWN),
                        new Length(0, Position.UNKNOWN),
                        Position.UNKNOWN)),
                new Wind(180, 5, Position.UNKNOWN),
                Position.UNKNOWN);
        Leg leg = leg(1, dep("OPO", "2026-06-01T08:00"), arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of(seg));
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(leg));

        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertEquals(2, c.errors().stream()
                .filter(e -> e.code() == SemanticErrorCode.NUMERIC_NOT_POSITIVE).count());
    }

    @Test
    @DisplayName("DomainConsistencyRule flags bad airport-code format")
    void domainConsistencyFlagsBadAirportFormat() {
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("LOWERCASE", "BAD"), List.of());
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.AIRPORT_CODE_BAD_FORMAT));
    }

    @Test
    @DisplayName("DomainConsistencyRule flags invalid calendar datetime in departure/arrival")
    void domainConsistencyFlagsBadDatetime() {
        Leg leg = leg(1,
                dep("OPO", "2026-02-30T08:00"),
                arr("LIS", "2026-13-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(leg));

        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertEquals(2, c.errors().stream()
                .filter(e -> e.code() == SemanticErrorCode.DATETIME_INVALID_CALENDAR).count());
    }

    @Test
    @DisplayName("DomainConsistencyRule labels flights without identifier as <missing>")
    void domainConsistencyHandlesNullFlightId() {
        Flight f = flightWith(null, FlightType.REGULAR,
                route("lowercase", "BAD"), List.of());
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.message().contains("<missing>")));
    }

    // ───────────────────────────────────────────────────────────────────────
    // RouteCoherenceRule
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("RouteCoherenceRule short-circuits when route is null")
    void routeCoherenceSkipsNullRoute() {
        Flight f = flightWith("TP1", FlightType.REGULAR, null,
                List.of(leg(1, dep("OPO", "2026-06-01T08:00"),
                        arr("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of())));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("RouteCoherenceRule short-circuits when legs are empty")
    void routeCoherenceSkipsEmptyLegs() {
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of());
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("RouteCoherenceRule tolerates null origin / destination / first leg")
    void routeCoherenceSkipsNullEdgeAirports() {
        Flight f = flightWith("TP1", FlightType.REGULAR,
                new Route(null, null, Position.UNKNOWN),
                List.of(leg(1,
                        new Departure(null, null, Position.UNKNOWN),
                        new Arrival(null, null, Position.UNKNOWN),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of())));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("RouteCoherenceRule reports origin mismatch")
    void routeCoherenceFlagsOriginMismatch() {
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("MAD", "LIS"),
                List.of(leg(1, dep("OPO", "2026-06-01T08:00"),
                        arr("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of())));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_ORIGIN_MISMATCH));
    }

    @Test
    @DisplayName("RouteCoherenceRule reports destination mismatch")
    void routeCoherenceFlagsDestinationMismatch() {
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "BCN"),
                List.of(leg(1, dep("OPO", "2026-06-01T08:00"),
                        arr("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of())));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_DESTINATION_MISMATCH));
    }

    @Test
    @DisplayName("RouteCoherenceRule reports repeated airport in route")
    void routeCoherenceFlagsRepeatedAirport() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("MAD", "2026-06-01T10:00"),
                arr("OPO", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "OPO"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_AIRPORT_REPEATED));
    }

    @Test
    @DisplayName("RouteCoherenceRule labels flights without identifier as <missing>")
    void routeCoherenceHandlesNullFlightId() {
        Flight f = flightWith(null, FlightType.REGULAR,
                route("MAD", "LIS"),
                List.of(leg(1, dep("OPO", "2026-06-01T08:00"),
                        arr("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                        List.of())));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.message().contains("<missing>")));
    }

    @Test
    @DisplayName("RouteCoherenceRule skips null AirportRef inside sequence")
    void routeCoherenceSkipsNullAirportInSequence() {
        Leg legA = leg(1,
                new Departure(null, TemporalLiteral.of("2026-06-01T08:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("MAD", "2026-06-01T10:00"),
                new Arrival(null, TemporalLiteral.of("2026-06-01T11:00", Position.UNKNOWN), Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route(null, null), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    // ───────────────────────────────────────────────────────────────────────
    // LegSequenceRule
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LegSequenceRule skips pair when current.arrival / next.departure are null")
    void legSequenceSkipsNullPieces() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"), null,
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, null, arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule skips pair when arrival/departure airports or codes are null")
    void legSequenceSkipsNullAirportsOrCodes() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                new Arrival(null, TemporalLiteral.of("2026-06-01T09:00", Position.UNKNOWN), Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(null, TemporalLiteral.of("2026-06-01T10:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule reports airport mismatch and time-not-ordered")
    void legSequenceFlagsBothBranches() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T10:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        // mismatch: next departs from BCN (not MAD), and time is before arrival.
        Leg legB = leg(2, dep("BCN", "2026-06-01T09:00"),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_AIRPORT_MISMATCH));
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("LegSequenceRule skips pair when temporal value is null (invalid calendar)")
    void legSequenceSkipsInvalidCalendar() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-02-30T10:00"), // invalid calendar -> value == null
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("MAD", "2026-06-01T11:00"),
                arr("LIS", "2026-06-01T12:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertFalse(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("LegSequenceRule labels flights without identifier as <missing>")
    void legSequenceHandlesNullFlightId() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T10:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("BCN", "2026-06-01T11:00"),
                arr("LIS", "2026-06-01T12:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith(null, FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.message().contains("<missing>")));
    }

    // ───────────────────────────────────────────────────────────────────────
    // FlightLevelRule
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FlightLevelRule reports duplicate flight identifiers")
    void flightLevelFlagsDuplicateId() {
        Flight a = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of());
        Flight b = flightWith("TP1", FlightType.REGULAR, route("OPO", "MAD"), List.of());
        ErrorCollector c = new ErrorCollector();
        new FlightLevelRule().check(fileOfNullableFlights(a, b), c);
        assertEquals(2, c.errors().stream()
                .filter(e -> e.code() == SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE).count());
    }

    @Test
    @DisplayName("FlightLevelRule reports null flight type")
    void flightLevelFlagsNullType() {
        Flight f = flightWith("TP1", null, route("OPO", "LIS"), List.of());
        ErrorCollector c = new ErrorCollector();
        new FlightLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.FLIGHT_TYPE_INVALID));
    }

    @Test
    @DisplayName("FlightLevelRule labels flights without identifier as <missing>")
    void flightLevelHandlesNullId() {
        Flight f = flightWith(null, null, route("OPO", "LIS"), List.of());
        ErrorCollector c = new ErrorCollector();
        new FlightLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream().anyMatch(e -> e.message().contains("<missing>")));
    }

    // ───────────────────────────────────────────────────────────────────────
    // safePos — every rule replaces a null Position with Position.UNKNOWN
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LegLevelRule.safePos falls back to UNKNOWN for null Position fields")
    void legLevelSafePosNullPosition() {
        Leg legWithNullPos = new Leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(-1, FuelUnit.KG, null), // null fuel position
                List.of(),
                null);
        Flight f = new Flight("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legWithNullPos), null);
        ErrorCollector c = new ErrorCollector();
        new LegLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.FUEL_NOT_POSITIVE));
    }

    @Test
    @DisplayName("SegmentCoherenceRule.safePos falls back to UNKNOWN for null segment Position")
    void segmentCoherenceSafePosNullPosition() {
        Segment seg = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                List.of(), null, null);
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of(seg));
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legA));

        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.SEGMENT_START_EQUALS_END));
    }

    @Test
    @DisplayName("DomainConsistencyRule.safePos falls back to UNKNOWN")
    void domainConsistencySafePosNullPosition() {
        Flight f = new Flight("TP1", FlightType.REGULAR,
                new Route(new AirportRef("BADCODE", null), null, Position.UNKNOWN),
                List.of(),
                null);
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.AIRPORT_CODE_BAD_FORMAT));
    }

    @Test
    @DisplayName("RouteCoherenceRule.safePos falls back to UNKNOWN when origin has null Position")
    void routeCoherenceSafePosNullPosition() {
        Flight f = new Flight("TP1", FlightType.REGULAR,
                new Route(new AirportRef("MAD", null), new AirportRef("LIS", null), null),
                List.of(leg(1, dep("OPO", "2026-06-01T08:00"),
                        arr("LIS", "2026-06-01T09:00"),
                        new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of())),
                Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_ORIGIN_MISMATCH));
    }

    @Test
    @DisplayName("FlightLevelRule.safePos falls back to UNKNOWN for null Flight Position")
    void flightLevelSafePosNullPosition() {
        Flight f = new Flight("TP1", null, route("OPO", "LIS"), List.of(), null);
        ErrorCollector c = new ErrorCollector();
        new FlightLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.FLIGHT_TYPE_INVALID));
    }

    @Test
    @DisplayName("LegSequenceRule.safePos falls back to UNKNOWN for null departure Position")
    void legSequenceSafePosNullPosition() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T10:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef("BCN", Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T11:00", Position.UNKNOWN),
                        null), // null position
                arr("LIS", "2026-06-01T12:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_AIRPORT_MISMATCH));
    }

    // ───────────────────────────────────────────────────────────────────────
    // Field-level null branches still missing
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LegLevelRule: segments == null is treated as LEG_MISSING_SEGMENTS")
    void legLevelFlagsNullSegments() {
        Leg legNullSegs = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                null);
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legNullSegs));
        ErrorCollector c = new ErrorCollector();
        new LegLevelRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.LEG_MISSING_SEGMENTS));
    }

    @Test
    @DisplayName("SegmentCoherenceRule: non-null flight with null legs is skipped")
    void segmentCoherenceSkipsFlightWithNullLegs() {
        Flight f = new Flight("TP1", FlightType.REGULAR, route("OPO", "LIS"), null, Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("SegmentCoherenceRule: non-null leg with null segments is skipped")
    void segmentCoherenceSkipsLegWithNullSegments() {
        Leg legNullSegs = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN),
                null);
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legNullSegs));
        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("SegmentCoherenceRule: segment with non-null start but null end is skipped")
    void segmentCoherenceSkipsSegmentWithNullEnd() {
        Segment segNullEnd = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                null,
                List.of(), null, Position.UNKNOWN);
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of(segNullEnd));
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legA));

        ErrorCollector c = new ErrorCollector();
        new SegmentCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("DomainConsistencyRule: non-null flight with null legs is skipped")
    void domainConsistencyNonNullFlightWithNullLegs() {
        Flight f = new Flight("TP1", FlightType.REGULAR, route("OPO", "LIS"), null, Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("DomainConsistencyRule: segment with null slots list is skipped (only wind is checked)")
    void domainConsistencySegmentWithNullSlots() {
        Segment seg = new Segment(
                new lapr4.lprog.flightdsl.ast.Coordinate(1, 2, Position.UNKNOWN),
                new lapr4.lprog.flightdsl.ast.Coordinate(3, 4, Position.UNKNOWN),
                null, // null slots
                new Wind(180, 5, Position.UNKNOWN),
                Position.UNKNOWN);
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of(seg));
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"), List.of(legA));
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("DomainConsistencyRule: airport ref with null code is skipped, ICAO 4-letter match accepted")
    void domainConsistencyAirportNullCodeAndIcaoMatch() {
        Flight f = flightWith("TP1", FlightType.REGULAR,
                new Route(new AirportRef(null, Position.UNKNOWN),
                        new AirportRef("LFPG", Position.UNKNOWN), Position.UNKNOWN),
                List.of());
        ErrorCollector c = new ErrorCollector();
        new DomainConsistencyRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("RouteCoherenceRule: non-null flight with null legs list is skipped")
    void routeCoherenceSkipsFlightWithNullLegs() {
        Flight f = new Flight("TP1", FlightType.REGULAR, route("OPO", "LIS"), null, Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("RouteCoherenceRule: first leg with null departure is treated as null firstDep")
    void routeCoherenceFirstLegNullDeparture() {
        Leg legA = leg(1, null, arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR, route("MAD", "LIS"),
                List.of(legA));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.ROUTE_ORIGIN_MISMATCH));
    }

    @Test
    @DisplayName("RouteCoherenceRule: last leg with null arrival is treated as null lastArr")
    void routeCoherenceLastLegNullArrival() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"), null,
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR, route("OPO", "LIS"),
                List.of(legA));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.ROUTE_DESTINATION_MISMATCH));
    }

    @Test
    @DisplayName("RouteCoherenceRule: airport ref with null code in sequence is skipped")
    void routeCoherenceSkipsNullCodeInSequence() {
        Leg legA = leg(1,
                new Departure(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T08:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("MAD", "2026-06-01T10:00"),
                new Arrival(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T11:00", Position.UNKNOWN), Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("MAD", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.ROUTE_AIRPORT_REPEATED));
    }

    @Test
    @DisplayName("RouteCoherenceRule.eq treats null route origin/destination code as no match")
    void routeCoherenceNullCodesNoMatch() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("LIS", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                new Route(new AirportRef(null, Position.UNKNOWN),
                        new AirportRef(null, Position.UNKNOWN), Position.UNKNOWN),
                List.of(legA));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_ORIGIN_MISMATCH));
        assertTrue(c.errors().stream()
                .anyMatch(e -> e.code() == SemanticErrorCode.ROUTE_DESTINATION_MISMATCH));
    }

    @Test
    @DisplayName("LegSequenceRule: non-null flight with null legs is skipped")
    void legSequenceFlightWithNullLegs() {
        Flight f = new Flight("TP1", FlightType.REGULAR, route("OPO", "LIS"), null, Position.UNKNOWN);
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule: pair where next is null but current is non-null is skipped")
    void legSequenceCurrentNonNullNextNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        final List<Leg> legs = new ArrayList<>();
        legs.add(legA);
        legs.add(null);
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), legs);
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule: airport-code pair with one code null is skipped")
    void legSequenceAirportCodeNullSkipped() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                new Arrival(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T09:00", Position.UNKNOWN), Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T10:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_AIRPORT_MISMATCH));
    }

    @Test
    @DisplayName("LegSequenceRule: pair where current arrival exists but next departure is null")
    void legSequenceCurrentArrNotNullNextDepNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, null,
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule: pair where dep.airport() is null but arr.airport() is not")
    void legSequenceArrAirportNotNullDepAirportNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(null, TemporalLiteral.of("2026-06-01T10:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule: pair where dep.airport().code() is null only")
    void legSequenceOnlyDepCodeNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T10:00", Position.UNKNOWN), Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().isEmpty());
    }

    @Test
    @DisplayName("LegSequenceRule: pair where arr.when() exists but dep.when() is null")
    void legSequenceArrWhenNotNullDepWhenNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef("MAD", Position.UNKNOWN), null, Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("LegSequenceRule: pair where only dep.when().value() is null")
    void legSequenceOnlyDepWhenValueNull() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef("MAD", Position.UNKNOWN),
                        TemporalLiteral.of("2026-02-30T10:00", Position.UNKNOWN), Position.UNKNOWN), // bad date
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }

    @Test
    @DisplayName("RouteCoherenceRule: sequence containing a null AirportRef entry is skipped")
    void routeCoherenceSkipsNullAirportEntry() {
        // Two legs where the first has departure airport set, second leg's arrival airport is
        // a non-null AirportRef but the .code() is null — so the entry is added to the sequence
        // but the inner check `a.code() == null` short-circuits.
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                arr("MAD", "2026-06-01T09:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2, dep("MAD", "2026-06-01T10:00"),
                new Arrival(new AirportRef(null, Position.UNKNOWN),
                        TemporalLiteral.of("2026-06-01T11:00", Position.UNKNOWN), Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new RouteCoherenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.ROUTE_AIRPORT_REPEATED));
    }

    @Test
    @DisplayName("LegSequenceRule: time pair with one when() null is skipped")
    void legSequenceTimeWhenNullSkipped() {
        Leg legA = leg(1, dep("OPO", "2026-06-01T08:00"),
                new Arrival(new AirportRef("MAD", Position.UNKNOWN), null, Position.UNKNOWN),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Leg legB = leg(2,
                new Departure(new AirportRef("MAD", Position.UNKNOWN), null, Position.UNKNOWN),
                arr("LIS", "2026-06-01T11:00"),
                new Fuel(5000, FuelUnit.KG, Position.UNKNOWN), List.of());
        Flight f = flightWith("TP1", FlightType.REGULAR,
                route("OPO", "LIS"), List.of(legA, legB));
        ErrorCollector c = new ErrorCollector();
        new LegSequenceRule().check(fileOfNullableFlights(f), c);
        assertTrue(c.errors().stream()
                .noneMatch(e -> e.code() == SemanticErrorCode.LEG_SEQUENCE_TIME_NOT_ORDERED));
    }
}
