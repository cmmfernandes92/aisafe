package eapli.aisafeweb.dsl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompilationResult;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanCompiler;
import eapli.aisafe.flightmanagement.application.parsing.FlightPlanIssue;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;
import eapli.aisafe.flightplanmanagement.dsl.CompilationResult;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporter;
import eapli.aisafe.flightplanmanagement.dsl.FlightPlanImporterFactory;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightLeg;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanDTO;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightSegment;
import eapli.aisafe.flightplanmanagement.dsl.dto.FuelUnit;
import eapli.aisafe.flightplanmanagement.dsl.validation.DslError;

/**
 * Web-side adapter implementing the core {@link FlightPlanCompiler} port using the
 * {@code aisafe.flightplandsl} compiler — the same importer US081/US121 run at
 * import time. It keeps {@code aisafe.core} DSL-unaware: core's US085 services ask
 * this port to turn the stored DSL blob back into a DSL-free {@link ParsedFlightPlan}.
 */
@Component
public class DslFlightPlanCompiler implements FlightPlanCompiler {

    /** JET A-1 density at 15°C (spec §3.3) — DSL litre→kg conversion at the boundary. */
    private static final double FUEL_DENSITY_KG_PER_L = 0.804;

    @Override
    public FlightPlanCompilationResult compile(final String rawDslContent) {
        if (rawDslContent == null || rawDslContent.isBlank()) {
            return FlightPlanCompilationResult.failed(List.of(
                    new FlightPlanIssue("LEXICAL", "EMPTY", "Flight plan content is empty", -1, -1)));
        }
        final FlightPlanImporter importer =
                FlightPlanImporterFactory.forPath(Path.of("plan.flight")).orElse(null);
        if (importer == null) {
            return FlightPlanCompilationResult.failed(List.of(
                    new FlightPlanIssue("LEXICAL", "NO_IMPORTER", "No DSL importer available", -1, -1)));
        }
        try (InputStream in = new ByteArrayInputStream(rawDslContent.getBytes(StandardCharsets.UTF_8))) {
            final CompilationResult result = importer.importFrom(in, "plan.flight");
            if (!result.ok()) {
                return FlightPlanCompilationResult.failed(mapIssues(result.errors()));
            }
            if (result.file().isEmpty() || result.file().get().flightPlans().isEmpty()) {
                return FlightPlanCompilationResult.failed(List.of(
                        new FlightPlanIssue("SEMANTIC", "NO_PLAN", "No flight plan found in DSL", -1, -1)));
            }
            return FlightPlanCompilationResult.ok(map(result.file().get().flightPlans().get(0)));
        } catch (final Exception e) {
            return FlightPlanCompilationResult.failed(List.of(
                    new FlightPlanIssue("LEXICAL", "IO", "Could not read flight plan: " + e.getMessage(), -1, -1)));
        }
    }

    private static List<FlightPlanIssue> mapIssues(final List<DslError> errors) {
        final List<FlightPlanIssue> issues = new ArrayList<>(errors.size());
        for (final DslError e : errors) {
            issues.add(new FlightPlanIssue(e.phase().name(), e.code(), e.message(), e.line(), e.column()));
        }
        return issues;
    }

    private static ParsedFlightPlan map(final FlightPlanDTO plan) {
        final List<ParsedSegment> segments = new ArrayList<>();
        for (final FlightLeg leg : plan.legs()) {
            final List<FlightSegment> legSegments = leg.segments();
            for (int i = 0; i < legSegments.size(); i++) {
                final FlightSegment s = legSegments.get(i);
                segments.add(new ParsedSegment(
                        leg.number(), i,
                        s.start().coordinates().latitude(), s.start().coordinates().longitude(),
                        s.end().coordinates().latitude(), s.end().coordinates().longitude(),
                        firstSlotAltitudeMeters(s),
                        s.windDirection().degrees().doubleValue(),
                        s.windSpeed().metersPerSecond().doubleValue()));
            }
        }
        final FlightLeg firstLeg = plan.legs().get(0);
        final FlightLeg lastLeg = plan.legs().get(plan.legs().size() - 1);
        final double fuelKg = totalFuelKg(plan);

        return new ParsedFlightPlan(
                plan.designator().value(),
                plan.route().origin().code(), plan.route().destination().code(),
                firstLeg.departure().when(), lastLeg.arrival().when(),
                fuelKg, ParsedFlightPlan.FuelUnit.KG,
                segments);
    }

    /** Total loaded fuel across all legs, normalised to kg. */
    private static double totalFuelKg(final FlightPlanDTO plan) {
        double kg = 0.0;
        for (final FlightLeg leg : plan.legs()) {
            final double qty = leg.fuel().quantity().doubleValue();
            kg += leg.fuel().unit() == FuelUnit.L ? qty * FUEL_DENSITY_KG_PER_L : qty;
        }
        return kg;
    }

    private static double firstSlotAltitudeMeters(final FlightSegment s) {
        return s.slots().isEmpty() ? 9000.0 : s.slots().get(0).altitude().meters().doubleValue();
    }
}