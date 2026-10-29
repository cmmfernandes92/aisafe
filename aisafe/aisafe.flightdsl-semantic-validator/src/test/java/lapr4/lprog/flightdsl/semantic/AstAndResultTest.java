package lapr4.lprog.flightdsl.semantic;

import lapr4.lprog.flightdsl.ast.Coordinate;
import lapr4.lprog.flightdsl.ast.Position;
import lapr4.lprog.flightdsl.ast.TemporalLiteral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AST helpers + SemanticValidationResult render branches")
class AstAndResultTest {

    @Test
    @DisplayName("Coordinate.samePointAs returns false for null other")
    void coordinateSamePointAsNull() {
        assertFalse(new Coordinate(0, 0, Position.UNKNOWN).samePointAs(null));
    }

    @Test
    @DisplayName("Coordinate.samePointAs is true when both lat and lon match")
    void coordinateSamePointAsMatch() {
        assertTrue(new Coordinate(38.78, -9.13, Position.UNKNOWN)
                .samePointAs(new Coordinate(38.78, -9.13, Position.UNKNOWN)));
    }

    @Test
    @DisplayName("Coordinate.samePointAs is false when latitudes differ")
    void coordinateSamePointAsLatDiffers() {
        assertFalse(new Coordinate(0, 0, Position.UNKNOWN)
                .samePointAs(new Coordinate(1, 0, Position.UNKNOWN)));
    }

    @Test
    @DisplayName("Coordinate.samePointAs is false when longitudes differ")
    void coordinateSamePointAsLonDiffers() {
        assertFalse(new Coordinate(0, 0, Position.UNKNOWN)
                .samePointAs(new Coordinate(0, 1, Position.UNKNOWN)));
    }

    @Test
    @DisplayName("TemporalLiteral.of returns a literal whose raw is null when given null")
    void temporalLiteralOfNull() {
        TemporalLiteral t = TemporalLiteral.of(null, Position.UNKNOWN);
        assertNull(t.raw());
        assertNull(t.value());
        assertFalse(t.isValidCalendar());
    }

    @Test
    @DisplayName("TemporalLiteral.of parses an input with seconds")
    void temporalLiteralOfWithSeconds() {
        TemporalLiteral t = TemporalLiteral.of("2026-06-01T08:30:45", Position.UNKNOWN);
        assertNotNull(t.value());
        assertTrue(t.isValidCalendar());
    }

    @Test
    @DisplayName("TemporalLiteral.of falls back to no-seconds when seconds parser fails")
    void temporalLiteralOfNoSeconds() {
        TemporalLiteral t = TemporalLiteral.of("2026-06-01T08:30", Position.UNKNOWN);
        assertNotNull(t.value());
        assertTrue(t.isValidCalendar());
    }

    @Test
    @DisplayName("TemporalLiteral.of returns null value for invalid calendar")
    void temporalLiteralOfBadCalendar() {
        TemporalLiteral t = TemporalLiteral.of("2026-02-30T10:00", Position.UNKNOWN);
        assertNull(t.value());
        assertFalse(t.isValidCalendar());
    }

    @Test
    @DisplayName("SemanticValidationResult.render lists findings on multiple lines")
    void resultRenderMultipleFindings() {
        List<SemanticError> findings = List.of(
                new SemanticError(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE, "first finding", Position.UNKNOWN),
                new SemanticError(SemanticErrorCode.FLIGHT_TYPE_INVALID, "second finding", Position.UNKNOWN));
        SemanticValidationResult result = new SemanticValidationResult(findings);

        String rendered = result.render();

        assertTrue(rendered.contains("first finding"));
        assertTrue(rendered.contains("second finding"));
        assertEquals(2, rendered.split("\n").length);
    }

    @Test
    @DisplayName("SemanticValidationResult.render returns OK marker when empty")
    void resultRenderEmpty() {
        SemanticValidationResult result = new SemanticValidationResult(List.of());

        assertEquals("OK (0 findings).", result.render());
    }

    @Test
    @DisplayName("SemanticValidationResult.has and count work for present and absent codes")
    void resultHasAndCount() {
        SemanticValidationResult result = new SemanticValidationResult(List.of(
                new SemanticError(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE, "x", Position.UNKNOWN),
                new SemanticError(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE, "y", Position.UNKNOWN)));

        assertTrue(result.has(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE));
        assertFalse(result.has(SemanticErrorCode.FLIGHT_TYPE_INVALID));
        assertEquals(2, result.count(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE));
        assertEquals(0, result.count(SemanticErrorCode.FLIGHT_TYPE_INVALID));
    }

    @Test
    @DisplayName("SemanticValidationResult.isValid is true only when empty")
    void resultIsValid() {
        assertTrue(new SemanticValidationResult(List.of()).isValid());
        assertFalse(new SemanticValidationResult(List.of(
                new SemanticError(SemanticErrorCode.FLIGHT_ID_NOT_UNIQUE, "x", Position.UNKNOWN)))
                .isValid());
    }
}
