package lapr4.lprog.flightdsl.ast;

import java.util.List;

/**
 * A flight segment with start/end coordinates, one or more altitude slots, and a wind block.
 *
 * <p>Semantic rules (§3.4.5 Segment coherence):
 * <ul>
 *   <li>{@code start} and {@code end} coordinates must be different.</li>
 *   <li>A segment must belong to exactly one leg (structurally enforced by the grammar; the
 *       validator additionally checks that the {@code slots} list is non-empty since the
 *       grammar's {@code slot+} guarantees ≥ 1 at parse time).</li>
 * </ul>
 */
public record Segment(
        Coordinate start,
        Coordinate end,
        List<Slot> slots,
        Wind wind,
        Position position
) { }
