package eapli.aisafe.flightplanmanagement.dsl;

import eapli.aisafe.flightplanmanagement.dsl.parsing.ParseStats;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public final class FlightDslCli {

    static IntConsumer exitHandler = System::exit;

    private FlightDslCli() {
    }

    public static void main(final String[] args) throws IOException {
        exitHandler.accept(run(args, System.out, System.err));
    }

    static int run(final String[] args, final PrintStream out, final PrintStream err) throws IOException {
        if (args.length != 1) {
            err.println("Usage: java -jar aisafe.flightplandsl.jar <flight-plan-file>");
            return 2;
        }

        final var file = Path.of(args[0]);
        final var compiler = new FlightDslCompiler();
        final var result = compiler.compile(file);

        if (result.ok()) {
            printSuccess(out, file, result, compiler.lastStats());
            return 0;
        }

        printErrors(out, file, result);
        return 1;
    }

    private static void printSuccess(final PrintStream out, final Path file,
                                     final CompilationResult result, final ParseStats stats) {
        out.println("Flight DSL validation: OK");
        out.println("File: " + file);
        out.println("Flights: " + stats.flights());
        out.println("Legs: " + stats.legs());
        out.println("Segments: " + stats.segments());
        out.println("Airports: " + stats.airportsReferenced().stream()
                .sorted()
                .collect(Collectors.joining(", ")));
        out.println("AST: " + result.file().orElseThrow().flightPlans().size()
                + " flight plan(s) produced");
    }

    private static void printErrors(final PrintStream out, final Path file, final CompilationResult result) {
        out.println("Flight DSL validation: FAILED");
        out.println("File: " + file);
        out.println("Errors: " + result.errors().size());
        for (final DslError error : result.errors()) {
            out.printf("[%s][%s] line %d:%d %s%n",
                    error.phase(),
                    error.code(),
                    error.line(),
                    error.column(),
                    error.message());
        }
    }
}
