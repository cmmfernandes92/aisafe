package eapli.aisafe.flightmanagement.application.parsing;

/**
 * Strategy port for turning a flight plan's stored DSL blob back into the
 * DSL-free {@link ParsedFlightPlan} that US085 (test) and the simulator view
 * consume. The flight-plan content is persisted only as raw text (US081/US121):
 * the parsed geometry, altitudes and baseline winds the physics needs are
 * recovered on demand by recompiling — exactly the "mapped domain graph" US081
 * deferred to US085.
 *
 * <p>Concrete implementations live in the layers that already depend on the DSL
 * compiler module (the console apps and the web adapter, which run US120/US081
 * validation at import time), so {@code aisafe.core} stays DSL-unaware — the same
 * hexagonal arrangement core uses for weather via {@code WeatherDataSourceReader}.
 * Implementations must run full lexical/syntactic/semantic analysis (US085.1) and
 * surface any problems as {@link FlightPlanIssue}s (US085.4).</p>
 */
@FunctionalInterface
public interface FlightPlanCompiler {

    /**
     * Compiles the given raw DSL content. Returns the parsed plan when valid, or
     * the located issues when not. Never throws for ordinary invalid input —
     * lexical/syntactic/semantic problems come back as issues.
     *
     * @param rawDslContent the stored flight-plan DSL blob (must not be empty)
     * @return the compilation result (parsed plan or located issues)
     */
    FlightPlanCompilationResult compile(String rawDslContent);
}