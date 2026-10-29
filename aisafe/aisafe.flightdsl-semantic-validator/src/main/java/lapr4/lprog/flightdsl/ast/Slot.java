package lapr4.lprog.flightdsl.ast;

/**
 * An altitude slot, matching {@code slot : SLOT LBRACE ALTITUDE COLON altitude=length WIDTH COLON width=length RBRACE}.
 *
 * <p>Both altitude and width must be strictly positive (§3.4.5 Domain consistency).
 */
public record Slot(Length altitude, Length width, Position position) { }
