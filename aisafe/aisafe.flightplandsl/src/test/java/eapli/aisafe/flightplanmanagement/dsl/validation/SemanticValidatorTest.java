package eapli.aisafe.flightplanmanagement.dsl.validation;

import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLLexer;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;
import eapli.aisafe.flightplanmanagement.dsl.parsing.CollectingErrorListener;
import eapli.aisafe.flightplanmanagement.dsl.parsing.FlightDomainBuildingVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "dsl/valid_simple.flight",
            "dsl/valid_multileg.flight",
            "dsl/valid_multisegment.flight"
    })
    void acceptsValidFixtures(final String resource) throws IOException {
        final var errors = validate(resource);

        assertThat(errors.all()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "dsl/invalid_S1_duplicate_flight_id.flight, S1",
            "dsl/invalid_S5_negative_fuel.flight, S5",
            "dsl/invalid_S6_leg_airport_mismatch.flight, S6",
            "dsl/invalid_S7_leg_time_overlap.flight, S7",
            "dsl/invalid_S8_route_origin_mismatch.flight, S8",
            "dsl/invalid_S9_route_destination_mismatch.flight, S9",
            "dsl/invalid_S10_airport_visited_twice.flight, S10",
            "dsl/invalid_S11_segment_zero_length.flight, S11",
            "dsl/invalid_S14_negative_altitude.flight, S14",
            "dsl/invalid_S14_negative_slot_width.flight, S14",
            "dsl/invalid_S14_negative_wind_speed.flight, S14",
            "dsl/invalid_S15_wind_direction_out_of_range.flight, S15",
            "dsl/invalid_S15_wind_direction_negative.flight, S15",
            "dsl/invalid_S16_bad_latitude.flight, S16",
            "dsl/invalid_S16_bad_longitude.flight, S16",
            "dsl/invalid_S16_latitude_low.flight, S16",
            "dsl/invalid_S16_longitude_low.flight, S16",
            "dsl/invalid_S17_bad_date.flight, S17"
    })
    void reportsExpectedRuleForDedicatedInvalidFixtures(final String resource,
                                                       final String expectedCode) throws IOException {
        final var errors = validate(resource);

        assertThat(errors.all())
                .extracting(DslError::code)
                .contains(expectedCode);
    }

    @ParameterizedTest
    @CsvSource({
            "dsl/invalid_multi_errors.flight, S5",
            "dsl/invalid_multi_errors.flight, S6",
            "dsl/invalid_multi_errors.flight, S15"
    })
    void collectsMultipleSemanticErrorsInOnePass(final String resource,
                                                 final String expectedCode) throws IOException {
        final var errors = validate(resource);

        assertThat(errors.all())
                .extracting(DslError::code)
                .contains(expectedCode);
    }

    @org.junit.jupiter.api.Test
    void detectsSegmentSharingLatitudeButNotLongitudeAsDifferent() throws IOException {
        final var errors = validate("dsl/invalid_S11_same_latitude.flight");

        assertThat(errors.all())
                .extracting(DslError::code)
                .doesNotContain("S11");
    }

    private ErrorCollector validate(final String resource) throws IOException {
        final var errors = new ErrorCollector();
        final FlightPlanFile file = build(resource, errors);
        assertThat(errors.has(DslError.Phase.LEXICAL)).isFalse();
        assertThat(errors.has(DslError.Phase.SYNTACTIC)).isFalse();

        new SemanticValidator(errors).validate(file);
        return errors;
    }

    private FlightPlanFile build(final String resource, final ErrorCollector errors) throws IOException {
        final var lexer = new FlightDSLLexer(CharStreams.fromStream(resource(resource)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.LEXICAL));

        final var parser = new FlightDSLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.SYNTACTIC));

        final var tree = parser.flightPlanFile();
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
