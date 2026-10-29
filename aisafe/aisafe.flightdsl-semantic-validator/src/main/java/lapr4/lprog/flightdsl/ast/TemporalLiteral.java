package lapr4.lprog.flightdsl.ast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * A datetime literal as it appears in the DSL ({@code DATETIME_LIT}).
 *
 * <p>The lexer accepts the lexical shape {@code YYYY-MM-DDTHH:MM[:SS]?} but does NOT
 * validate calendar correctness (e.g. {@code 2026-02-30} matches the regex but is not a
 * real date). The validator must enforce this — §3.4.5 Domain consistency:
 * <em>"Date and time values must represent valid calendar values."</em>
 *
 * <p>Use {@link #of(String, Position)} to build one; {@link #isValidCalendar()} is true
 * iff {@code raw} parsed successfully into {@link #value()}.
 */
public record TemporalLiteral(String raw, LocalDateTime value, Position position) {

    private static final DateTimeFormatter WITH_SECONDS = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NO_SECONDS   = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Build from a raw lexeme, attempting strict ISO-like parse. {@code value} is null if invalid. */
    public static TemporalLiteral of(String raw, Position position) {
        if (raw == null) return new TemporalLiteral(null, null, position);
        LocalDateTime parsed = tryParse(raw, WITH_SECONDS);
        if (parsed == null) parsed = tryParse(raw, NO_SECONDS);
        return new TemporalLiteral(raw, parsed, position);
    }

    private static LocalDateTime tryParse(String s, DateTimeFormatter fmt) {
        try {
            return LocalDateTime.parse(s, fmt);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isValidCalendar() { return value != null; }
}