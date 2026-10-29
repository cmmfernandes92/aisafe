package eapli.aisafe.flightmanagement.application.parsing;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of compiling a flight plan's DSL through the {@link FlightPlanCompiler}
 * port: either the {@link ParsedFlightPlan} (when no issues) or the list of
 * {@link FlightPlanIssue}s found. Mirrors the DSL module's {@code CompilationResult}
 * in DSL-free terms so US085 can re-validate (US085.1) and report errors with type
 * and location (US085.4) without depending on the DSL module.
 */
public record FlightPlanCompilationResult(Optional<ParsedFlightPlan> plan, List<FlightPlanIssue> issues) {

    public FlightPlanCompilationResult {
        if (plan == null) {
            throw new IllegalArgumentException("plan optional must not be null");
        }
        issues = (issues == null) ? List.of() : List.copyOf(issues);
    }

    public static FlightPlanCompilationResult ok(final ParsedFlightPlan plan) {
        return new FlightPlanCompilationResult(Optional.of(plan), List.of());
    }

    public static FlightPlanCompilationResult failed(final List<FlightPlanIssue> issues) {
        return new FlightPlanCompilationResult(Optional.empty(), issues);
    }

    public boolean ok() {
        return issues.isEmpty() && plan.isPresent();
    }
}