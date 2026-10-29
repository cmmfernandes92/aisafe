package lapr4.lprog.flightdsl.ast;

/**
 * Geographic coordinate (latitude, longitude) in decimal degrees, as parsed from
 * {@code coordinates : LPAREN lat=number COMMA lon=number RPAREN}.
 */
public record Coordinate(double latitude, double longitude, Position position) {

    /** Two coordinates are <em>semantically</em> the same point iff both components match exactly. */
    public boolean samePointAs(Coordinate other) {
        return other != null
                && Double.compare(this.latitude, other.latitude) == 0
                && Double.compare(this.longitude, other.longitude) == 0;
    }
}