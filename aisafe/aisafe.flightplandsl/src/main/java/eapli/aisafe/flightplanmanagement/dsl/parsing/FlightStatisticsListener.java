package eapli.aisafe.flightplanmanagement.dsl.parsing;

import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLBaseListener;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;

import java.util.Set;
import java.util.TreeSet;

public class FlightStatisticsListener extends FlightDSLBaseListener {

    private int flights;
    private int legs;
    private int segments;
    private final Set<String> airportsReferenced = new TreeSet<>();

    @Override
    public void enterFlight(final FlightDSLParser.FlightContext ctx) {
        flights++;
    }

    @Override
    public void enterLeg(final FlightDSLParser.LegContext ctx) {
        legs++;
    }

    @Override
    public void enterSegment(final FlightDSLParser.SegmentContext ctx) {
        segments++;
    }

    @Override
    public void enterAirportCode(final FlightDSLParser.AirportCodeContext ctx) {
        airportsReferenced.add(ctx.getText());
    }

    public ParseStats snapshot() {
        return new ParseStats(flights, legs, segments, airportsReferenced);
    }
}
