package eapli.aisafe.flightplanmanagement.dsl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class FlightDslCliTest {

    private final IntConsumer originalExitHandler = FlightDslCli.exitHandler;

    @AfterEach
    void restoreExitHandler() {
        FlightDslCli.exitHandler = originalExitHandler;
    }

    @Test
    void printsReadableSuccessSummary() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var err = new ByteArrayOutputStream();

        final int exitCode = FlightDslCli.run(
                new String[]{"src/test/resources/dsl/valid_multileg.flight"},
                new PrintStream(out),
                new PrintStream(err));

        assertThat(exitCode).isZero();
        assertThat(err.toString()).isEmpty();
        assertThat(out.toString())
                .contains("Flight DSL validation: OK")
                .contains("Flights: 1")
                .contains("Legs: 2")
                .contains("Segments: 2")
                .contains("Airports: LEMD, LPPR, LPPT")
                .contains("AST: 1 flight plan(s) produced");
    }

    @Test
    void printsErrorsForInvalidFile() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var err = new ByteArrayOutputStream();

        final int exitCode = FlightDslCli.run(
                new String[]{"src/test/resources/dsl/invalid_multi_errors.flight"},
                new PrintStream(out),
                new PrintStream(err));

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).isEmpty();
        assertThat(out.toString())
                .contains("Flight DSL validation: FAILED")
                .contains("[SEMANTIC][S5]")
                .contains("[SEMANTIC][S6]")
                .contains("[SEMANTIC][S15]");
    }

    @Test
    void mainDelegatesExitCodeToExitHandler() throws IOException {
        final AtomicInteger captured = new AtomicInteger(-1);
        FlightDslCli.exitHandler = captured::set;

        FlightDslCli.main(new String[]{});

        assertThat(captured.get()).isEqualTo(2);
    }

    @Test
    void mainPropagatesZeroExitCodeForValidFile() throws IOException {
        final AtomicInteger captured = new AtomicInteger(-1);
        FlightDslCli.exitHandler = captured::set;

        FlightDslCli.main(new String[]{ "src/test/resources/dsl/valid_simple.flight" });

        assertThat(captured.get()).isZero();
    }

    @Test
    void printsUsageWhenArgumentsAreInvalid() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var err = new ByteArrayOutputStream();

        final int exitCode = FlightDslCli.run(new String[]{}, new PrintStream(out), new PrintStream(err));

        assertThat(exitCode).isEqualTo(2);
        assertThat(out.toString()).isEmpty();
        assertThat(err.toString()).contains("Usage:");
    }
}
