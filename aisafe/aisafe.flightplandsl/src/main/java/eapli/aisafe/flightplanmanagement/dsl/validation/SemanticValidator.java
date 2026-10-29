package eapli.aisafe.flightplanmanagement.dsl.validation;

import eapli.aisafe.flightplanmanagement.dsl.dto.AltitudeSlot;
import eapli.aisafe.flightplanmanagement.dsl.dto.Coordinates;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightLeg;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanDTO;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightPlanFile;
import eapli.aisafe.flightplanmanagement.dsl.dto.FlightSegment;
import eapli.aisafe.flightplanmanagement.dsl.dto.SourceLocation;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SemanticValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal FULL_CIRCLE_DEGREES = BigDecimal.valueOf(360);

    private final ErrorCollector errors;

    public SemanticValidator(final ErrorCollector errors) {
        this.errors = errors;
    }

    public void validate(final FlightPlanFile file) {
        checkUniqueFlightIds(file);
        for (final FlightPlanDTO flight : file.flightPlans()) {
            checkFuelPositivity(flight);
            checkLegSequenceCoherence(flight);
            checkRouteCoherence(flight);
            checkSegments(flight);
            checkWind(flight);
            checkCoordinates(flight);
        }
    }

    private void checkUniqueFlightIds(final FlightPlanFile file) {
        final Map<String, List<FlightPlanDTO>> grouped = file.flightPlans().stream()
                .collect(Collectors.groupingBy(flight -> flight.designator().value()));

        grouped.forEach((id, flights) -> {
            if (flights.size() > 1) {
                for (int i = 1; i < flights.size(); i++) {
                    add("S1", "duplicate flight designator '" + id + "'", flights.get(i).location());
                }
            }
        });
    }

    private void checkFuelPositivity(final FlightPlanDTO flight) {
        for (final FlightLeg leg : flight.legs()) {
            if (leg.fuel().quantity().compareTo(ZERO) <= 0) {
                add("S5", "leg " + leg.number() + " fuel quantity must be strictly positive",
                        leg.fuel().location());
            }
        }
    }

    private void checkLegSequenceCoherence(final FlightPlanDTO flight) {
        final List<FlightLeg> legs = flight.legs();
        for (int i = 0; i < legs.size() - 1; i++) {
            final FlightLeg current = legs.get(i);
            final FlightLeg next = legs.get(i + 1);

            if (!Objects.equals(current.arrival().airport().code(), next.departure().airport().code())) {
                add("S6",
                        "leg " + next.number() + " departs from " + next.departure().airport().code()
                                + " but previous leg arrives at " + current.arrival().airport().code(),
                        next.departure().location());
            }
            if (!next.departure().when().isAfter(current.arrival().when())) {
                add("S7",
                        "leg " + next.number() + " departure time is not after leg " + current.number()
                                + " arrival",
                        next.departure().location());
            }
        }
    }

    private void checkRouteCoherence(final FlightPlanDTO flight) {
        final List<FlightLeg> legs = flight.legs();
        final FlightLeg first = legs.getFirst();
        final FlightLeg last = legs.getLast();

        if (!Objects.equals(flight.route().origin().code(), first.departure().airport().code())) {
            add("S8",
                    "route origin " + flight.route().origin().code()
                            + " does not match first leg departure " + first.departure().airport().code(),
                    flight.route().location());
        }
        if (!Objects.equals(flight.route().destination().code(), last.arrival().airport().code())) {
            add("S9",
                    "route destination " + flight.route().destination().code()
                            + " does not match last leg arrival " + last.arrival().airport().code(),
                    flight.route().location());
        }

        checkRepeatedAirports(flight);
    }

    private void checkRepeatedAirports(final FlightPlanDTO flight) {
        final var visited = new HashSet<String>();
        final var firstDeparture = flight.legs().getFirst().departure().airport();
        visited.add(firstDeparture.code());

        for (final FlightLeg leg : flight.legs()) {
            final var arrival = leg.arrival().airport();
            if (!visited.add(arrival.code())) {
                add("S10", "airport " + arrival.code() + " is visited more than once",
                        leg.arrival().location());
            }
        }
    }

    private void checkSegments(final FlightPlanDTO flight) {
        for (final FlightLeg leg : flight.legs()) {
            for (final FlightSegment segment : leg.segments()) {
                if (sameCoordinates(segment.start().coordinates(), segment.end().coordinates())) {
                    add("S11", "segment start and end coordinates must be different", segment.location());
                }

                for (final AltitudeSlot slot : segment.slots()) {
                    if (slot.altitude().meters().compareTo(ZERO) <= 0) {
                        add("S14", "slot altitude must be strictly positive", slot.altitude().location());
                    }
                    if (slot.width().meters().compareTo(ZERO) <= 0) {
                        add("S14", "slot width must be strictly positive", slot.width().location());
                    }
                }
                if (segment.windSpeed().metersPerSecond().compareTo(ZERO) <= 0) {
                    add("S14", "wind speed must be strictly positive", segment.windSpeed().location());
                }
            }
        }
    }

    private void checkWind(final FlightPlanDTO flight) {
        for (final FlightLeg leg : flight.legs()) {
            for (final FlightSegment segment : leg.segments()) {
                final BigDecimal degrees = segment.windDirection().degrees();
                if (degrees.compareTo(ZERO) < 0 || degrees.compareTo(FULL_CIRCLE_DEGREES) >= 0) {
                    add("S15", "wind direction must be in [0, 360)", segment.windDirection().location());
                }
            }
        }
    }

    private void checkCoordinates(final FlightPlanDTO flight) {
        for (final FlightLeg leg : flight.legs()) {
            for (final FlightSegment segment : leg.segments()) {
                checkCoordinate(segment.start().coordinates());
                checkCoordinate(segment.end().coordinates());
            }
        }
    }

    private void checkCoordinate(final Coordinates coordinates) {
        if (coordinates.latitude() < -90 || coordinates.latitude() > 90) {
            add("S16", "latitude must be in [-90, 90]", coordinates.location());
        }
        if (coordinates.longitude() < -180 || coordinates.longitude() > 180) {
            add("S16", "longitude must be in [-180, 180]", coordinates.location());
        }
    }

    private boolean sameCoordinates(final Coordinates left, final Coordinates right) {
        return Double.compare(left.latitude(), right.latitude()) == 0
                && Double.compare(left.longitude(), right.longitude()) == 0;
    }

    private void add(final String code, final String message, final SourceLocation location) {
        errors.add(new DslError(DslError.Phase.SEMANTIC, code, message,
                location.line(), location.column()));
    }
}
