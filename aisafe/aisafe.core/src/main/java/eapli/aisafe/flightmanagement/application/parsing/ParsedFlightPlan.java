package eapli.aisafe.flightmanagement.application.parsing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A flight plan recovered from its DSL blob as plain values — the DSL-free input
 * the US085 physics test and the simulator view consume. The DSL-backed
 * {@code FlightPlanCompiler} adapter produces it; {@code aisafe.core} never
 * imports the DSL module (mirroring the weather {@code WeatherDataSourceReader}
 * port). Geometry, altitudes and baseline winds live in {@link #segments()};
 * {@link #fuelQuantity()}/{@link #fuelUnit()} carry the loaded fuel as authored,
 * so the physics layer owns the litre→kg conversion.
 *
 * <p>For a multi-leg plan the segments of every leg are flattened in order (each
 * keeps its own {@code legNumber}/{@code segmentIndex}); {@link #departure()} is
 * the first leg's departure and {@link #arrival()} the last leg's arrival.</p>
 */
public record ParsedFlightPlan(
        String designator,
        String originCode,
        String destinationCode,
        LocalDateTime departure,
        LocalDateTime arrival,
        double fuelQuantity,
        FuelUnit fuelUnit,
        List<ParsedSegment> segments) {

    /** Fuel unit as authored in the DSL; the physics layer normalises to kg. */
    public enum FuelUnit { KG, L }

    public ParsedFlightPlan {
        if (designator == null || designator.isBlank()) {
            throw new IllegalArgumentException("Plan designator must not be empty");
        }
        if (departure == null || arrival == null) {
            throw new IllegalArgumentException("Plan departure and arrival must be present");
        }
        if (fuelQuantity <= 0) {
            throw new IllegalArgumentException("Loaded fuel must be positive");
        }
        if (fuelUnit == null) {
            throw new IllegalArgumentException("Fuel unit must be present");
        }
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("A flight plan must have at least one segment");
        }
        segments = List.copyOf(segments);
    }
}