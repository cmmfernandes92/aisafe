package lapr4.lprog.flightdsl.ast;

/**
 * Line/column information carried from the parser to enable accurate error reporting.
 *
 * <p>Per §3.4.4 of the project requirements: <em>"Produce clear and informative error
 * messages, including: Error type; Line and column information."</em>
 *
 * <p>{@link #UNKNOWN} should only be used when constructing AST fragments programmatically
 * (e.g. in tests) where source position is not meaningful.
 */
public record Position(int line, int column) {

    public static final Position UNKNOWN = new Position(-1, -1);

    public boolean isKnown() {
        return line > 0;
    }

    @Override
    public String toString() {
        return isKnown() ? "line " + line + ", column " + column : "<unknown position>";
    }
}