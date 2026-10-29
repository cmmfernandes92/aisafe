package eapli.aisafe.flightmanagement.application.areasimulation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EquirectangularProjectionTest {

    @Test
    public void projectsReferencePointToOrigin() {
        final EquirectangularProjection projection = new EquirectangularProjection(41.0, -6.0);

        final EquirectangularProjection.Point point = projection.project(41.0, -6.0);

        assertEquals(0.0, point.x(), 1e-9);
        assertEquals(0.0, point.y(), 1e-9);
    }

    @Test
    public void projectsOneLatitudeDegreeLikeScompFormula() {
        final EquirectangularProjection projection = new EquirectangularProjection(0.0, 0.0);

        final EquirectangularProjection.Point point = projection.project(1.0, 0.0);

        assertEquals(0.0, point.x(), 1e-9);
        assertEquals(Math.PI / 180.0 * EquirectangularProjection.EARTH_RADIUS_M, point.y(), 1e-6);
    }
}
