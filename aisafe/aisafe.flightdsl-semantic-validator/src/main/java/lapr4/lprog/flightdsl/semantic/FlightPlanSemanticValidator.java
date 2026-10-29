package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.FlightPlanFile;
import lapr4.lprog.flightdsl.semantic.rules.DomainConsistencyRule;
import lapr4.lprog.flightdsl.semantic.rules.FlightLevelRule;
import lapr4.lprog.flightdsl.semantic.rules.LegLevelRule;
import lapr4.lprog.flightdsl.semantic.rules.LegSequenceRule;
import lapr4.lprog.flightdsl.semantic.rules.RouteCoherenceRule;
import lapr4.lprog.flightdsl.semantic.rules.SegmentCoherenceRule;
import lapr4.lprog.flightdsl.semantic.rules.SemanticRule;

import java.util.List;

/**
 * Entry point for the semantic validation of a Flight DSL AST.
 *
 * <p>Implements §3.4.5 of the AISafe project requirements. Consumes a
 * {@link FlightPlanFile} produced by the ANTLR parser (US083) and returns a
 * {@link SemanticValidationResult} with every finding from every rule family.
 *
 * <p>All rules run in one pass and write to the shared {@link ErrorCollector},
 * fulfilling the §3.4.5 requirement that <em>"all semantic errors must be
 * collected and reported in a single execution."</em>
 */
public final class FlightPlanSemanticValidator {

    private static final List<SemanticRule> RULES = List.of(
            new FlightLevelRule(),
            new LegLevelRule(),
            new LegSequenceRule(),
            new RouteCoherenceRule(),
            new SegmentCoherenceRule(),
            new DomainConsistencyRule()
    );

    public SemanticValidationResult validate(FlightPlanFile file) {
        ErrorCollector collector = new ErrorCollector();
        if (file != null) {
            for (SemanticRule rule : RULES) {
                rule.check(file, collector);
            }
        }
        return new SemanticValidationResult(collector.errors());
    }
}