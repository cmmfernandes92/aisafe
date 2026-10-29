package eapli.aisafe.flightplanmanagement.dsl.grammar;

import eapli.aisafe.flightplanmanagement.dsl.parsing.CollectingErrorListener;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "dsl/valid_simple.flight",
            "dsl/valid_multileg.flight",
            "dsl/valid_multisegment.flight",
            "dsl/invalid_S1_duplicate_flight_id.flight",
            "dsl/invalid_S5_negative_fuel.flight",
            "dsl/invalid_S6_leg_airport_mismatch.flight",
            "dsl/invalid_S7_leg_time_overlap.flight",
            "dsl/invalid_S8_route_origin_mismatch.flight",
            "dsl/invalid_S9_route_destination_mismatch.flight",
            "dsl/invalid_S10_airport_visited_twice.flight",
            "dsl/invalid_S11_segment_zero_length.flight",
            "dsl/invalid_S14_negative_altitude.flight",
            "dsl/invalid_S15_wind_direction_out_of_range.flight",
            "dsl/invalid_S16_bad_latitude.flight",
            "dsl/invalid_S17_bad_date.flight",
            "dsl/invalid_multi_errors.flight"
    })
    void parsesSyntacticallyValidFixtures(final String resource) throws IOException {
        final var collector = parse(resource);

        assertThat(collector.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dsl/invalid_syntax_missing_brace.flight"})
    void reportsSyntacticErrors(final String resource) throws IOException {
        final var collector = parse(resource);

        assertThat(collector.has(DslError.Phase.LEXICAL)).isFalse();
        assertThat(collector.has(DslError.Phase.SYNTACTIC)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dsl/invalid_lexical_lowercase_iata.flight"})
    void reportsLexicalErrors(final String resource) throws IOException {
        final var collector = parse(resource);

        assertThat(collector.has(DslError.Phase.LEXICAL)).isTrue();
    }

    private ErrorCollector parse(final String resource) throws IOException {
        final var collector = new ErrorCollector();
        final var lexer = new FlightDSLLexer(CharStreams.fromStream(resource(resource)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(collector, DslError.Phase.LEXICAL));

        final var parser = new FlightDSLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(collector, DslError.Phase.SYNTACTIC));
        parser.flightPlanFile();
        return collector;
    }

    private InputStream resource(final String resource) {
        final var stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Missing test resource: " + resource);
        }
        return stream;
    }
}
