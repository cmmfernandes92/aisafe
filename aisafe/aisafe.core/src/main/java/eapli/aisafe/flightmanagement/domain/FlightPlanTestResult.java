package eapli.aisafe.flightmanagement.domain;

/**
 * Outcome of a {@link FlightPlanTestService} run.
 * Immutable value object returned to the application layer and ultimately to
 * the UI adapter.
 */
public final class FlightPlanTestResult {

    private final boolean success;
    private final String message;

    private FlightPlanTestResult(final boolean success, final String message) {
        this.success = success;
        this.message = message;
    }

    public static FlightPlanTestResult success(final String message) {
        return new FlightPlanTestResult(true, message);
    }

    public static FlightPlanTestResult failure(final String message) {
        return new FlightPlanTestResult(false, message != null ? message : "Test failed");
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String message() {
        return this.message;
    }

    @Override
    public String toString() {
        return (this.success ? "SUCCESS" : "FAILURE") + ": " + this.message;
    }
}
