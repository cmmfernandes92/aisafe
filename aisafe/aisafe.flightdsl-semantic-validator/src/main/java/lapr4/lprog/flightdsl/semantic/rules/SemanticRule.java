package lapr4.lprog.flightdsl.semantic.rules;

import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.semantic.ErrorCollector;

/** A semantic-rule family. Walks the AST and appends findings to the shared collector. */
public interface SemanticRule {
    void check(FlightPlanFile file, ErrorCollector collector);
}