package eapli.aisafe.reporting.domain;

import eapli.framework.domain.model.ValueObject;

/**
 * A point in the simulator's projected local frame: {@code x}/{@code y} in metres
 * and altitude in metres. Derived on read from a {@link SafetyViolation}'s stored
 * coordinates; not persisted on its own.
 */
public record Position(double x, double y, double altitude) implements ValueObject {
}