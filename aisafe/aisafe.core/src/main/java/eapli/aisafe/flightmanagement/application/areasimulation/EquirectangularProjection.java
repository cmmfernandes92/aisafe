package eapli.aisafe.flightmanagement.application.areasimulation;

public class EquirectangularProjection {

    public static final double EARTH_RADIUS_M = 6371000.0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private final double refLat;
    private final double refLon;

    public EquirectangularProjection(final double refLat, final double refLon) {
        this.refLat = refLat;
        this.refLon = refLon;
    }

    public Point project(final double lat, final double lon) {
        final double latRad = lat * DEG_TO_RAD;
        final double refLatRad = refLat * DEG_TO_RAD;
        final double x = EARTH_RADIUS_M * (lon - refLon) * DEG_TO_RAD
                * Math.cos((latRad + refLatRad) / 2.0);
        final double y = EARTH_RADIUS_M * (lat - refLat) * DEG_TO_RAD;
        return new Point(x, y);
    }

    public static double haversineMeters(final double lat1, final double lon1,
            final double lat2, final double lon2) {
        final double dlat = (lat2 - lat1) * DEG_TO_RAD;
        final double dlon = (lon2 - lon1) * DEG_TO_RAD;
        final double a = Math.sin(dlat / 2.0) * Math.sin(dlat / 2.0)
                + Math.cos(lat1 * DEG_TO_RAD) * Math.cos(lat2 * DEG_TO_RAD)
                * Math.sin(dlon / 2.0) * Math.sin(dlon / 2.0);
        final double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_M * c;
    }

    public record Point(double x, double y) {
    }
}
