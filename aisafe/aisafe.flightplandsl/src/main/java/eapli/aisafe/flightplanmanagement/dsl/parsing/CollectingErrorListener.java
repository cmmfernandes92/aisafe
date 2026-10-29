package eapli.aisafe.flightplanmanagement.dsl.parsing;

import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class CollectingErrorListener extends BaseErrorListener {

    private final ErrorCollector collector;
    private final DslError.Phase phase;

    public CollectingErrorListener(final ErrorCollector collector, final DslError.Phase phase) {
        this.collector = collector;
        this.phase = phase;
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
                            final int line, final int charPositionInLine, final String msg,
                            final RecognitionException e) {
        collector.add(new DslError(phase, code(), msg, line, charPositionInLine));
    }

    private String code() {
        return phase == DslError.Phase.LEXICAL ? "LEX" : "PARSE";
    }
}
