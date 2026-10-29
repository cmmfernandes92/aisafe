package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Position;

/**
 * A single semantic finding carrying the three things §3.4.4 requires:
 * an error type (the {@link SemanticErrorCode}), a human-readable message,
 * and source position (line/column).
 */
public record SemanticError(SemanticErrorCode code, String message, Position position) {

    /** Render as "FLIGHT_ID_NOT_UNIQUE @ line 12, column 8: message". */
    public String render() {
        return code + " @ " + position + ": " + message;
    }

    @Override
    public String toString() { return render(); }
}
