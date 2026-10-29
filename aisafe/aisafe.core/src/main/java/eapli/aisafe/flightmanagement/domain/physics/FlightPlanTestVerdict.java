package eapli.aisafe.flightmanagement.domain.physics;

import java.util.List;

/**
 * The outcome of running the US085 physics test on a flight plan: whether it
 * passed and, if not, the list of clear failure reasons (DSL errors with type and
 * location per US085.4, fuel insufficiency, altitude breaches, or a departure-time
 * mismatch). A failing verdict must leave the plan's state untouched (US085.5).
 */
public record FlightPlanTestVerdict(boolean passed, List<String> failures, String summary) {

    public FlightPlanTestVerdict {
        failures = (failures == null) ? List.of() : List.copyOf(failures);
    }

    public static FlightPlanTestVerdict passed(final String summary) {
        return new FlightPlanTestVerdict(true, List.of(), summary);
    }

    public static FlightPlanTestVerdict failed(final List<String> failures) {
        final String summary = (failures == null || failures.isEmpty())
                ? "Flight plan test failed"
                : "Flight plan test failed: " + String.join("; ", failures);
        return new FlightPlanTestVerdict(false, failures, summary);
    }
}