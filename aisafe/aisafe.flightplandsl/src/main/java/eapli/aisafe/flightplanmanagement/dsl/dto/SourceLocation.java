package eapli.aisafe.flightplanmanagement.dsl.dto;

public record SourceLocation(int line, int column) {

    public static final SourceLocation UNKNOWN = new SourceLocation(-1, -1);
}
