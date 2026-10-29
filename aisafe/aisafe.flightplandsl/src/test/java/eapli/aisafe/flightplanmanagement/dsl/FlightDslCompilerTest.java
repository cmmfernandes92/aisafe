package eapli.aisafe.flightplanmanagement.dsl;

import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class FlightDslCompilerTest {

    @Test
    void compilesValidFileAndReturnsAst() throws IOException {
        final var compiler = new FlightDslCompiler();

        final var result = compiler.compile(Path.of("src/test/resources/dsl/valid_simple.flight"));

        assertThat(result.ok()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.file()).isPresent();
        assertThat(result.file().orElseThrow().flightPlans().getFirst().designator().value()).isEqualTo("TP123");
    }

    @Test
    void exposesStatisticsForLastSuccessfulParse() throws IOException {
        final var compiler = new FlightDslCompiler();

        compiler.compile(Path.of("src/test/resources/dsl/valid_multileg.flight"));

        assertThat(compiler.lastStats().flights()).isEqualTo(1);
        assertThat(compiler.lastStats().legs()).isEqualTo(2);
        assertThat(compiler.lastStats().segments()).isEqualTo(2);
        assertThat(compiler.lastStats().airportsReferenced()).containsExactlyInAnyOrder("LPPR", "LPPT", "LEMD");
    }

    @Test
    void returnsErrorsAndNoAstForLexicalFailures() throws IOException {
        final var compiler = new FlightDslCompiler();

        final var result = compiler.compile(Path.of("src/test/resources/dsl/invalid_lexical_lowercase_iata.flight"));

        assertThat(result.ok()).isFalse();
        assertThat(result.file()).isEmpty();
        assertThat(result.errors()).extracting(DslError::phase).contains(DslError.Phase.LEXICAL);
    }

    @Test
    void returnsErrorsAndNoAstForSyntaxFailures() throws IOException {
        final var compiler = new FlightDslCompiler();

        final var result = compiler.compile(Path.of("src/test/resources/dsl/invalid_syntax_missing_brace.flight"));

        assertThat(result.ok()).isFalse();
        assertThat(result.file()).isEmpty();
        assertThat(result.errors()).extracting(DslError::phase).contains(DslError.Phase.SYNTACTIC);
    }

    @Test
    void collectsMultipleSemanticErrorsAndReturnsNoAst() throws IOException {
        final var compiler = new FlightDslCompiler();

        final var result = compiler.compile(Path.of("src/test/resources/dsl/invalid_multi_errors.flight"));

        assertThat(result.ok()).isFalse();
        assertThat(result.file()).isEmpty();
        assertThat(result.errors())
                .extracting(DslError::code)
                .contains("S5", "S6", "S15");
    }

    @Test
    void importerContractAcceptsInputStream() throws IOException {
        final FlightPlanImporter importer = new FlightDslCompiler();

        try (InputStream stream = resource("dsl/valid_simple.flight")) {
            final var result = importer.importFrom(stream, "valid_simple.flight");

            assertThat(result.ok()).isTrue();
            assertThat(result.file()).isPresent();
        }
    }

    @Test
    void importerContractCanCompileInlineContent() {
        final FlightPlanImporter importer = new FlightDslCompiler();
        final var bytes = """
                flight TP999 {
                  type: regular
                  route { origin: LPPT destination: LFPG }
                  leg 1 {
                    departure { airport: LPPT datetime: 2026-05-20T08:30 }
                    arrival { airport: LFPG datetime: 2026-05-20T11:45 }
                    fuel: 8500 kg
                    segments {
                      segment {
                        start: (38.78, -9.13)
                        end: (48.99, 2.55)
                        slots { slot { altitude: 10000 m width: 500 m } }
                        wind { direction: 270 speed: 15.5 m/s }
                      }
                    }
                  }
                }
                """.getBytes();

        final var result = importer.importFrom(new ByteArrayInputStream(bytes), "inline.flight");

        assertThat(result.ok()).isTrue();
        assertThat(result.file()).isPresent();
    }

    @Test
    void skipsSemanticValidationWhenVisitorAlreadyAddedAnError() throws IOException {
        final var compiler = new FlightDslCompiler();

        final var result = compiler.compile(Path.of("src/test/resources/dsl/invalid_S17_bad_date.flight"));

        assertThat(result.ok()).isFalse();
        assertThat(result.errors()).extracting(DslError::code).contains("S17");
    }

    @Test
    void reportsIoErrorWhenStreamThrowsWithNullSourceName() {
        final FlightPlanImporter importer = new FlightDslCompiler();
        final InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("disk error"); }
        };

        final var result = importer.importFrom(broken, null);

        assertThat(result.ok()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().phase()).isEqualTo(DslError.Phase.LEXICAL);
        assertThat(result.errors().getFirst().code()).isEqualTo("IO");
        assertThat(result.errors().getFirst().message()).contains("input");
    }

    @Test
    void reportsIoErrorWhenStreamThrowsWithBlankSourceName() {
        final FlightPlanImporter importer = new FlightDslCompiler();
        final InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("disk error"); }
        };

        final var result = importer.importFrom(broken, "   ");

        assertThat(result.ok()).isFalse();
        assertThat(result.errors().getFirst().message()).contains("input");
    }

    @Test
    void reportsIoErrorWhenStreamThrowsWithNamedSource() {
        final FlightPlanImporter importer = new FlightDslCompiler();
        final InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("disk error"); }
        };

        final var result = importer.importFrom(broken, "my_plan.flight");

        assertThat(result.ok()).isFalse();
        assertThat(result.errors().getFirst().message()).contains("my_plan.flight");
    }

    private InputStream resource(final String resource) {
        return Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resource),
                "missing test resource " + resource);
    }
}
