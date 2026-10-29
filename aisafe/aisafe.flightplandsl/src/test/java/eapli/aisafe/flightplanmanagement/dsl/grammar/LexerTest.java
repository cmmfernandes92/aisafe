package eapli.aisafe.flightplanmanagement.dsl.grammar;

import eapli.aisafe.flightplanmanagement.dsl.parsing.CollectingErrorListener;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

    @Test
    void tokenisesKeywordsCaseInsensitively() {
        assertThat(tokenTypes("flight Flight FLIGHT")).containsExactly(
                FlightDSLLexer.FLIGHT,
                FlightDSLLexer.FLIGHT,
                FlightDSLLexer.FLIGHT);

        assertThat(tokenTypes("type regular charter route origin destination leg departure arrival airport datetime"))
                .containsExactly(
                        FlightDSLLexer.TYPE,
                        FlightDSLLexer.REGULAR,
                        FlightDSLLexer.CHARTER,
                        FlightDSLLexer.ROUTE,
                        FlightDSLLexer.ORIGIN,
                        FlightDSLLexer.DESTINATION,
                        FlightDSLLexer.LEG,
                        FlightDSLLexer.DEPARTURE,
                        FlightDSLLexer.ARRIVAL,
                        FlightDSLLexer.AIRPORT,
                        FlightDSLLexer.DATETIME);

        assertThat(tokenTypes("fuel segments segment start end slots slot altitude width wind direction speed"))
                .containsExactly(
                        FlightDSLLexer.FUEL,
                        FlightDSLLexer.SEGMENTS,
                        FlightDSLLexer.SEGMENT,
                        FlightDSLLexer.START,
                        FlightDSLLexer.END,
                        FlightDSLLexer.SLOTS,
                        FlightDSLLexer.SLOT,
                        FlightDSLLexer.ALTITUDE,
                        FlightDSLLexer.WIDTH,
                        FlightDSLLexer.WIND,
                        FlightDSLLexer.DIRECTION,
                        FlightDSLLexer.SPEED);
    }

    @Test
    void tokenisesUnitsCaseInsensitively() {
        assertThat(tokenTypes("kg Kg KG l L m M m/s M/S")).containsExactly(
                FlightDSLLexer.KG_UNIT,
                FlightDSLLexer.KG_UNIT,
                FlightDSLLexer.KG_UNIT,
                FlightDSLLexer.L_UNIT,
                FlightDSLLexer.L_UNIT,
                FlightDSLLexer.M_UNIT,
                FlightDSLLexer.M_UNIT,
                FlightDSLLexer.MS_UNIT,
                FlightDSLLexer.MS_UNIT);
    }

    @Test
    void tokenisesFlightIdsAirportCodesNumbersAndDatetimes() {
        assertThat(tokenTypes("TP123 LH4205 AF77A LIS LPPT 2026-05-20T08:30 100 -50 38.78 -9.13"))
                .containsExactly(
                        FlightDSLLexer.FLIGHT_ID,
                        FlightDSLLexer.FLIGHT_ID,
                        FlightDSLLexer.FLIGHT_ID,
                        FlightDSLLexer.IATA_CODE,
                        FlightDSLLexer.ICAO_CODE,
                        FlightDSLLexer.DATETIME_LIT,
                        FlightDSLLexer.INT,
                        FlightDSLLexer.INT,
                        FlightDSLLexer.FLOAT,
                        FlightDSLLexer.FLOAT);
    }

    @Test
    void lowerCaseAirportCodeProducesLexicalErrors() {
        final var collector = new ErrorCollector();
        final var lexer = new FlightDSLLexer(CharStreams.fromString("lis"));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CollectingErrorListener(collector, DslError.Phase.LEXICAL));

        final List<? extends Token> tokens = lexer.getAllTokens();

        assertThat(tokens).extracting(Token::getType).doesNotContain(FlightDSLLexer.IATA_CODE);
        assertThat(collector.has(DslError.Phase.LEXICAL)).isTrue();
    }

    private List<Integer> tokenTypes(final String input) {
        final var lexer = new FlightDSLLexer(CharStreams.fromString(input));
        return lexer.getAllTokens().stream()
                .map(Token::getType)
                .toList();
    }
}
