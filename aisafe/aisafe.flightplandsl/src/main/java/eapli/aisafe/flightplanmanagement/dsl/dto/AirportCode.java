package eapli.aisafe.flightplanmanagement.dsl.dto;

public sealed interface AirportCode permits AirportIATACode, AirportICAOCode {

    String code();

    SourceLocation location();
}
