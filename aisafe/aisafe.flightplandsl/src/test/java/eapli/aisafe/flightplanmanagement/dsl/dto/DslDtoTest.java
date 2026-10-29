package eapli.aisafe.flightplanmanagement.dsl.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslDtoTest {

    @Test
    void sourceLocationUnknownUsesSentinelCoordinates() {
        assertThat(SourceLocation.UNKNOWN.line()).isEqualTo(-1);
        assertThat(SourceLocation.UNKNOWN.column()).isEqualTo(-1);
    }

    @Test
    void airportCodesKeepDistinctIataAndIcaoTypes() {
        final AirportCode iata = new AirportIATACode("LIS", location());
        final AirportCode icao = new AirportICAOCode("LPPT", location());

        assertThat(iata).isInstanceOf(AirportIATACode.class);
        assertThat(iata.code()).isEqualTo("LIS");
        assertThat(icao).isInstanceOf(AirportICAOCode.class);
        assertThat(icao.code()).isEqualTo("LPPT");
    }

    @Test
    void recordsRejectNullRequiredValues() {
        assertThatNullPointerException().isThrownBy(() -> new FlightDesignator(null));
        assertThatNullPointerException().isThrownBy(() -> new AirportIATACode(null, location()));
        assertThatNullPointerException().isThrownBy(() -> new Length(null, location()));
        assertThatNullPointerException().isThrownBy(() -> new WindSpeed(BigDecimal.ONE, null));
    }

    @Test
    void listBasedRecordsDefensivelyCopyCollections() {
        final var legs = new ArrayList<FlightLeg>();
        final var plan = new FlightPlanDTO(
                new FlightDesignator("TP123"),
                FlightType.REGULAR,
                route("LPPT", "LFPG"),
                legs,
                location());

        legs.add(leg());

        assertThat(plan.legs()).isEmpty();
        assertThatThrownBy(() -> plan.legs().add(leg()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void dtoCoordinatesDoNotFailFastOnSemanticInvalidValues() {
        assertThatCode(() -> new Coordinates(95.0, 0.0, location()))
                .doesNotThrowAnyException();
    }

    @Test
    void canRepresentACompleteFlightPlanTree() {
        final var file = new FlightPlanFile(List.of(new FlightPlanDTO(
                new FlightDesignator("TP123"),
                FlightType.REGULAR,
                route("LPPT", "LFPG"),
                List.of(leg()),
                location())));

        assertThat(file.flightPlans()).hasSize(1);
        assertThat(file.flightPlans().getFirst().legs()).hasSize(1);
    }

    private FlightLeg leg() {
        final var departure = new DepartureInfo(
                new AirportICAOCode("LPPT", location()),
                LocalDateTime.parse("2026-05-20T08:30"),
                location());
        final var arrival = new ArrivalInfo(
                new AirportICAOCode("LFPG", location()),
                LocalDateTime.parse("2026-05-20T11:45"),
                location());
        final var fuel = new FuelLoad(BigDecimal.valueOf(8500), FuelUnit.KG, location());
        return new FlightLeg(1, departure, arrival, fuel, List.of(segment()), location());
    }

    private FlightSegment segment() {
        return new FlightSegment(
                new Waypoint(new Coordinates(38.78, -9.13, location()), location()),
                new Waypoint(new Coordinates(48.99, 2.55, location()), location()),
                List.of(new AltitudeSlot(
                        new Length(BigDecimal.valueOf(10000), location()),
                        new Length(BigDecimal.valueOf(500), location()),
                        location())),
                new WindDirection(BigDecimal.valueOf(270), location()),
                new WindSpeed(BigDecimal.valueOf(15.5), location()),
                location());
    }

    private Route route(final String origin, final String destination) {
        return new Route(
                new AirportICAOCode(origin, location()),
                new AirportICAOCode(destination, location()),
                location());
    }

    private SourceLocation location() {
        return new SourceLocation(1, 0);
    }
}
