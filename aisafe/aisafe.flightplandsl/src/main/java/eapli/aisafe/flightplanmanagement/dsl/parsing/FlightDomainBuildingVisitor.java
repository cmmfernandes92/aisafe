package eapli.aisafe.flightplanmanagement.dsl.parsing;

import eapli.aisafe.flightplanmanagement.dsl.dto.AirportCode;
import eapli.aisafe.flightplanmanagement.dsl.dto.AirportIATACode;
import eapli.aisafe.flightplanmanagement.dsl.dto.AirportICAOCode;
import eapli.aisafe.flightplanmanagement.dsl.dto.AltitudeSlot;
import eapli.aisafe.flightplanmanagement.dsl.dto.ArrivalInfo;
import eapli.aisafe.flightplanmanagement.dsl.dto.Coordinates;
import eapli.aisafe.flightplanmanagement.dsl.dto.DepartureInfo;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightDesignator;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightLeg;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanDTO;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightSegment;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightType;
import eapli.aisafe.flightplanmanagement.dsl.dto.FuelLoad;
import eapli.aisafe.flightplanmanagement.dsl.dto.FuelUnit;
import eapli.aisafe.flightplanmanagement.dsl.dto.Length;
import eapli.aisafe.flightplanmanagement.dsl.dto.Route;
import eapli.aisafe.flightplanmanagement.dsl.dto.SourceLocation;
import eapli.aisafe.flightplanmanagement.dsl.dto.Waypoint;
import eapli.aisafe.flightplanmanagement.dsl.dto.WindDirection;
import eapli.aisafe.flightplanmanagement.dsl.dto.WindSpeed;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLBaseVisitor;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class FlightDomainBuildingVisitor extends FlightDSLBaseVisitor<Object> {

    private final ErrorCollector errors;

    public FlightDomainBuildingVisitor() {
        this(new ErrorCollector());
    }

    public FlightDomainBuildingVisitor(final ErrorCollector errors) {
        this.errors = errors;
    }

    public ErrorCollector errors() {
        return errors;
    }

    @Override
    public FlightPlanFile visitFlightPlanFile(final FlightDSLParser.FlightPlanFileContext ctx) {
        final List<FlightPlanDTO> plans = ctx.flight().stream()
                .map(context -> (FlightPlanDTO) visit(context))
                .toList();
        return new FlightPlanFile(plans);
    }

    @Override
    public FlightPlanDTO visitFlight(final FlightDSLParser.FlightContext ctx) {
        final var designator = new FlightDesignator(ctx.id.getText());
        final var type = FlightType.valueOf(ctx.flightType().value.getText().toUpperCase(Locale.ROOT));
        final var route = (Route) visit(ctx.route());
        final List<FlightLeg> legs = ctx.leg().stream()
                .map(context -> (FlightLeg) visit(context))
                .toList();
        return new FlightPlanDTO(designator, type, route, legs, loc(ctx));
    }

    @Override
    public Route visitRoute(final FlightDSLParser.RouteContext ctx) {
        return new Route(
                (AirportCode) visit(ctx.origin),
                (AirportCode) visit(ctx.destination),
                loc(ctx));
    }

    @Override
    public FlightLeg visitLeg(final FlightDSLParser.LegContext ctx) {
        final List<FlightSegment> segments = ctx.segmentsBlock().segment().stream()
                .map(context -> (FlightSegment) visit(context))
                .toList();
        return new FlightLeg(
                Integer.parseInt(ctx.legNumber.getText()),
                (DepartureInfo) visit(ctx.departure()),
                (ArrivalInfo) visit(ctx.arrival()),
                (FuelLoad) visit(ctx.fuel()),
                segments,
                loc(ctx));
    }

    @Override
    public DepartureInfo visitDeparture(final FlightDSLParser.DepartureContext ctx) {
        return new DepartureInfo(
                (AirportCode) visit(ctx.airport),
                parseDateTime(ctx.dt),
                loc(ctx));
    }

    @Override
    public ArrivalInfo visitArrival(final FlightDSLParser.ArrivalContext ctx) {
        return new ArrivalInfo(
                (AirportCode) visit(ctx.airport),
                parseDateTime(ctx.dt),
                loc(ctx));
    }

    @Override
    public FuelLoad visitFuel(final FlightDSLParser.FuelContext ctx) {
        return new FuelLoad(
                toBigDecimal(ctx.quantity),
                (FuelUnit) visit(ctx.unit),
                loc(ctx));
    }

    @Override
    public FuelUnit visitFuelUnit(final FlightDSLParser.FuelUnitContext ctx) {
        return ctx.KG_UNIT() != null ? FuelUnit.KG : FuelUnit.L;
    }

    @Override
    public FlightSegment visitSegment(final FlightDSLParser.SegmentContext ctx) {
        final var start = new Waypoint((Coordinates) visit(ctx.startCoords), loc(ctx.startCoords));
        final var end = new Waypoint((Coordinates) visit(ctx.endCoords), loc(ctx.endCoords));
        final List<AltitudeSlot> slots = ctx.slotsBlock().slot().stream()
                .map(context -> (AltitudeSlot) visit(context))
                .toList();
        final var wind = ctx.wind();
        return new FlightSegment(
                start,
                end,
                slots,
                new WindDirection(toBigDecimal(wind.direction), loc(wind)),
                new WindSpeed(toBigDecimal(wind.speed), loc(wind)),
                loc(ctx));
    }

    @Override
    public AltitudeSlot visitSlot(final FlightDSLParser.SlotContext ctx) {
        return new AltitudeSlot(
                (Length) visit(ctx.altitude),
                (Length) visit(ctx.width),
                loc(ctx));
    }

    @Override
    public Coordinates visitCoordinates(final FlightDSLParser.CoordinatesContext ctx) {
        return new Coordinates(
                toBigDecimal(ctx.lat).doubleValue(),
                toBigDecimal(ctx.lon).doubleValue(),
                loc(ctx));
    }

    @Override
    public Length visitLength(final FlightDSLParser.LengthContext ctx) {
        return new Length(toBigDecimal(ctx.value), loc(ctx));
    }

    @Override
    public AirportCode visitAirportCode(final FlightDSLParser.AirportCodeContext ctx) {
        if (ctx.IATA_CODE() != null) {
            return new AirportIATACode(ctx.IATA_CODE().getText(), loc(ctx));
        }
        return new AirportICAOCode(ctx.ICAO_CODE().getText(), loc(ctx));
    }

    private LocalDateTime parseDateTime(final Token token) {
        try {
            return LocalDateTime.parse(token.getText());
        } catch (final DateTimeParseException e) {
            errors.add(new DslError(
                    DslError.Phase.SEMANTIC,
                    "S17",
                    "invalid datetime '" + token.getText() + "'",
                    token.getLine(),
                    token.getCharPositionInLine()));
            return LocalDateTime.MIN;
        }
    }

    private static BigDecimal toBigDecimal(final FlightDSLParser.NumberContext ctx) {
        return new BigDecimal(ctx.getText());
    }

    private static SourceLocation loc(final ParserRuleContext ctx) {
        final Token token = ctx.getStart();
        return new SourceLocation(token.getLine(), token.getCharPositionInLine());
    }
}
