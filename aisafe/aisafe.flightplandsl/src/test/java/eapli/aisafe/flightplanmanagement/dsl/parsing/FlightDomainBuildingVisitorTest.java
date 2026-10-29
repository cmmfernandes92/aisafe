package eapli.aisafe.flightplanmanagement.dsl.parsing;

import eapli.aisafe.flightplanmanagement.dsl.dto.AirportIATACode;
import eapli.aisafe.flightplanmanagement.dsl.dto.AirportICAOCode;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightType;
import eapli.aisafe.flightplanmanagement.dsl.dto.FuelUnit;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLLexer;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FlightDomainBuildingVisitorTest {

    @Test
    void buildsSimpleFlightPlanFile() throws IOException {
        final var file = build("dsl/valid_simple.flight");

        assertThat(file.flightPlans()).hasSize(1);
        final var plan = file.flightPlans().getFirst();
        assertThat(plan.designator().value()).isEqualTo("TP123");
        assertThat(plan.type()).isEqualTo(FlightType.REGULAR);
        assertThat(plan.route().origin()).isInstanceOf(AirportICAOCode.class);
        assertThat(plan.route().origin().code()).isEqualTo("LPPT");
        assertThat(plan.route().destination().code()).isEqualTo("LFPG");

        final var leg = plan.legs().getFirst();
        assertThat(leg.number()).isEqualTo(1);
        assertThat(leg.departure().airport().code()).isEqualTo("LPPT");
        assertThat(leg.departure().when()).isEqualTo(LocalDateTime.parse("2026-05-20T08:30"));
        assertThat(leg.arrival().airport().code()).isEqualTo("LFPG");
        assertThat(leg.fuel().quantity()).isEqualByComparingTo(BigDecimal.valueOf(8500));
        assertThat(leg.fuel().unit()).isEqualTo(FuelUnit.KG);

        final var segment = leg.segments().getFirst();
        assertThat(segment.start().coordinates().latitude()).isEqualTo(38.78);
        assertThat(segment.start().coordinates().longitude()).isEqualTo(-9.13);
        assertThat(segment.end().coordinates().latitude()).isEqualTo(48.99);
        assertThat(segment.slots()).hasSize(2);
        assertThat(segment.slots().getFirst().altitude().meters()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(segment.windDirection().degrees()).isEqualByComparingTo(BigDecimal.valueOf(270));
        assertThat(segment.windSpeed().metersPerSecond()).isEqualByComparingTo(BigDecimal.valueOf(15.5));
        assertThat(plan.location().line()).isEqualTo(2);
    }

    @Test
    void buildsMultiLegFlightPlanFile() throws IOException {
        final var file = build("dsl/valid_multileg.flight");
        final var plan = file.flightPlans().getFirst();

        assertThat(plan.designator().value()).isEqualTo("TP200");
        assertThat(plan.legs()).hasSize(2);
        assertThat(plan.legs().getFirst().arrival().airport().code()).isEqualTo("LPPT");
        assertThat(plan.legs().get(1).departure().airport().code()).isEqualTo("LPPT");
        assertThat(plan.legs().get(1).arrival().airport().code()).isEqualTo("LEMD");
        assertThat(plan.legs().get(1).fuel().quantity()).isEqualByComparingTo(BigDecimal.valueOf(4800));
    }

    @Test
    void buildsMultiSegmentFlightPlanFile() throws IOException {
        final var file = build("dsl/valid_multisegment.flight");
        final var plan = file.flightPlans().getFirst();
        final var leg = plan.legs().getFirst();

        assertThat(plan.designator().value()).isEqualTo("LH4205");
        assertThat(plan.type()).isEqualTo(FlightType.CHARTER);
        assertThat(leg.segments()).hasSize(3);
        assertThat(leg.segments().get(1).slots()).hasSize(2);
        assertThat(leg.segments().get(2).end().coordinates().longitude()).isEqualTo(-73.78);
        assertThat(leg.segments().get(2).windSpeed().metersPerSecond()).isEqualByComparingTo(BigDecimal.valueOf(22.5));
    }

    @Test
    void buildsFlightPlanWithIataAirportCodes() throws IOException {
        final var file = build("dsl/valid_simple_iata.flight");
        final var plan = file.flightPlans().getFirst();

        assertThat(plan.route().origin()).isInstanceOf(AirportIATACode.class);
        assertThat(plan.route().origin().code()).isEqualTo("LIS");
        assertThat(plan.route().destination()).isInstanceOf(AirportIATACode.class);
        assertThat(plan.route().destination().code()).isEqualTo("CDG");
    }

    @Test
    void parsesFuelUnitInLiters() throws IOException {
        final var file = build("dsl/valid_simple_liters.flight");
        final var plan = file.flightPlans().getFirst();

        assertThat(plan.legs().getFirst().fuel().unit()).isEqualTo(FuelUnit.L);
    }

    @Test
    void defaultConstructorWiresAnEmptyErrorCollector() {
        final var visitor = new FlightDomainBuildingVisitor();

        assertThat(visitor.errors()).isNotNull();
        assertThat(visitor.errors().all()).isEmpty();
    }

    @Test
    void reportsInvalidCalendarDatesDuringAstConstruction() throws IOException {
        final var errors = new ErrorCollector();

        build("dsl/invalid_S17_bad_date.flight", errors);

        assertThat(errors.all())
                .anySatisfy(error -> {
                    assertThat(error.phase()).isEqualTo(DslError.Phase.SEMANTIC);
                    assertThat(error.code()).isEqualTo("S17");
                });
    }

    private FlightPlanFile build(final String resource) throws IOException {
        return build(resource, new ErrorCollector());
    }

    private FlightPlanFile build(final String resource, final ErrorCollector errors) throws IOException {
        final var lexer = new FlightDSLLexer(CharStreams.fromStream(resource(resource)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.LEXICAL));

        final var parser = new FlightDSLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.SYNTACTIC));
        final var tree = parser.flightPlanFile();

        assertThat(errors.has(DslError.Phase.LEXICAL)).isFalse();
        assertThat(errors.has(DslError.Phase.SYNTACTIC)).isFalse();
        return new FlightDomainBuildingVisitor(errors).visitFlightPlanFile(tree);
    }

    private InputStream resource(final String resource) {
        final var stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Missing test resource: " + resource);
        }
        return stream;
    }
}
