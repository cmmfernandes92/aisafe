package eapli.aisafe.flightplanmanagement.dsl;

import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLLexer;
import eapli.aisafe.flightplanmanagement.dsl.grammar.FlightDSLParser;
import eapli.aisafe.flightplanmanagement.dsl.parsing.CollectingErrorListener;
import eapli.aisafe.flightplanmanagement.dsl.parsing.FlightDomainBuildingVisitor;
import eapli.aisafe.flightplanmanagement.dsl.parsing.FlightStatisticsListener;
import eapli.aisafe.flightplanmanagement.dsl.parsing.ParseStats;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import eapli.aisafe.flightplanmanagement.dsl.validation.ErrorCollector;
import eapli.aisafe.flightplanmanagement.dsl.validation.SemanticValidator;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FlightDslCompiler implements FlightPlanImporter {

    private ParseStats lastStats = new ParseStats(0, 0, 0, java.util.Set.of());

    @Override
    public CompilationResult importFrom(final InputStream content, final String sourceName) {
        final var errors = new ErrorCollector();
        try {
            final var lexer = new FlightDSLLexer(CharStreams.fromStream(content));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.LEXICAL));

            final var parser = new FlightDSLParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(new CollectingErrorListener(errors, DslError.Phase.SYNTACTIC));

            final var tree = parser.flightPlanFile();
            if (errors.hasErrors()) {
                return new CompilationResult(Optional.empty(), errors.all());
            }

            final var statsListener = new FlightStatisticsListener();
            ParseTreeWalker.DEFAULT.walk(statsListener, tree);
            lastStats = statsListener.snapshot();

            final FlightPlanFile file = new FlightDomainBuildingVisitor(errors).visitFlightPlanFile(tree);
            if (!errors.hasErrors()) {
                new SemanticValidator(errors).validate(file);
            }

            return new CompilationResult(errors.hasErrors() ? Optional.empty() : Optional.of(file), errors.all());
        } catch (final IOException e) {
            errors.add(new DslError(
                    DslError.Phase.LEXICAL,
                    "IO",
                    "failed to read " + displayName(sourceName) + ": " + e.getMessage(),
                    -1,
                    -1));
            return new CompilationResult(Optional.empty(), errors.all());
        }
    }

    public CompilationResult compile(final Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return importFrom(in, file.getFileName().toString());
        }
    }

    public ParseStats lastStats() {
        return lastStats;
    }

    private String displayName(final String sourceName) {
        return sourceName == null || sourceName.isBlank() ? "input" : sourceName;
    }
}
