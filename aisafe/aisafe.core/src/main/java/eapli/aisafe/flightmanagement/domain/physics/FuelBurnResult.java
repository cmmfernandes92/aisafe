package eapli.aisafe.flightmanagement.domain.physics;

import java.util.List;

/**
 * Result of integrating the §3.3 fuel burn over a flight plan (US085): the trip
 * burn, the fuel remaining at arrival, the block time, and a sampled
 * fuel-vs-progress timeline the simulator can render. All masses in kg.
 */
public record FuelBurnResult(
        double loadedFuelKg,
        double tripBurnKg,
        double fuelRemainingKg,
        double blockSeconds,
        List<Sample> timeline) {

    /** One point on the fuel-vs-progress curve: route fraction, fuel left, altitude. */
    public record Sample(double progress, double fuelKg, double altitudeMeters) {
    }

    public FuelBurnResult {
        timeline = (timeline == null) ? List.of() : List.copyOf(timeline);
    }
}