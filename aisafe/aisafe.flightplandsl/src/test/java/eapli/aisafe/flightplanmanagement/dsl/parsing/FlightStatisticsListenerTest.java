package eapli.aisafe.flightplanmanagement.dsl.parsing;

import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLLexer;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FlightStatisticsListenerTest {

    @ParameterizedTest
    @CsvSource({
            "dsl/valid_simple.flight, 1, 1, 1",
            "dsl/valid_multileg.flight, 1, 2, 2",
            "dsl/valid_multisegment.flight, 1, 1, 3"
    })
    void gathersFlightLegAndSegmentCounts(final String resource, final int flights,
                                          final int legs, final int segments) throws IOException {
        final var stats = statsFor(resource);

        assertThat(stats.flights()).isEqualTo(flights);
        assertThat(stats.legs()).isEqualTo(legs);
        assertThat(stats.segments()).isEqualTo(segments);
    }

    @Test
    void gathersReferencedAirports() throws IOException {
        final var stats = statsFor("dsl/valid_multileg.flight");

        assertThat(stats.airportsReferenced()).containsExactlyInAnyOrder("LPPR", "LPPT", "LEMD");
    }

    private ParseStats statsFor(final String resource) throws IOException {
        final var lexer = new FlightDSLLexer(CharStreams.fromStream(resource(resource)));
        final var parser = new FlightDSLParser(new CommonTokenStream(lexer));
        final var tree = parser.flightPlanFile();

        final var listener = new FlightStatisticsListener();
        ParseTreeWalker.DEFAULT.walk(listener, tree);
        return listener.snapshot();
    }

    private InputStream resource(final String resource) {
        final var stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Missing test resource: " + resource);
        }
        return stream;
    }
}
