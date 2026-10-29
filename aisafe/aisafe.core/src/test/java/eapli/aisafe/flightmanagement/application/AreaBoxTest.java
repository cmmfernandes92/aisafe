package eapli.aisafe.flightmanagement.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AirControlArea;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.AreaName;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;
import eapli.aisafe.aircontrolareamanagement.domain.GeographicBoundary;
import eapli.aisafe.flightmanagement.application.parsing.ParsedFlightPlan;
import eapli.aisafe.flightmanagement.application.parsing.ParsedSegment;

public class AreaBoxTest {

    private static AirControlArea area() {
        return new AirControlArea(AreaCode.valueOf("LPPC"), AreaName.valueOf("Lisbon ACC"),
                new GeographicBoundary(new Coordinates(38.0, -9.5), new Coordinates(39.0, -8.5)));
    }

    private static ParsedSegment seg(final double sLat, final double sLon,
            final double eLat, final double eLon) {
        return new ParsedSegment(1, 0, sLat, sLon, eLat, eLon, 9000.0, 270.0, 5.0);
    }

    private static ParsedFlightPlan plan(final ParsedSegment... segments) {
        return new ParsedFlightPlan("FR401", "LPPR", "LPPT",
                LocalDateTime.of(2026, 6, 12, 13, 0), LocalDateTime.of(2026, 6, 12, 15, 0),
                5000.0, ParsedFlightPlan.FuelUnit.KG, List.of(segments));
    }

    @Test
    public void ofDerivesBoundsFromBoundary() {
        final AreaBox box = AreaBox.of(area());
        assertEquals(38.0, box.minLat(), 1e-9);
        assertEquals(39.0, box.maxLat(), 1e-9);
        assertEquals(-9.5, box.minLon(), 1e-9);
        assertEquals(-8.5, box.maxLon(), 1e-9);
    }

    @Test
    public void containsIsEdgeInclusive() {
        final AreaBox box = AreaBox.of(area());
        assertTrue(box.contains(38.5, -9.0));   // interior
        assertTrue(box.contains(38.0, -9.5));   // corner
        assertFalse(box.contains(40.0, -9.0));  // north of box
        assertFalse(box.contains(38.5, -10.0)); // west of box
    }

    @Test
    public void touchesWhenEitherEndpointInside() {
        final AreaBox box = AreaBox.of(area());
        assertTrue(box.touches(seg(38.5, -9.0, 50.0, 2.0)));  // start inside
        assertTrue(box.touches(seg(50.0, 2.0, 38.5, -9.0)));  // end inside
        assertFalse(box.touches(seg(50.0, 2.0, 51.0, 3.0)));  // neither
    }

    @Test
    public void touchesWhenSegmentCrossesAreaWithoutEndpointInside() {
        final AreaBox box = AreaBox.of(area());

        assertTrue(box.touches(seg(38.5, -10.0, 38.5, -8.0)));
        assertEquals(1, box.segmentsInside(plan(seg(38.5, -10.0, 38.5, -8.0))));
    }

    @Test
    public void segmentsInsideCountsTouchingSegments() {
        final AreaBox box = AreaBox.of(area());
        final ParsedFlightPlan p = plan(
                seg(38.5, -9.0, 38.6, -9.1),  // inside
                seg(50.0, 2.0, 51.0, 3.0),    // outside
                seg(60.0, 5.0, 38.7, -9.2));  // end inside
        assertEquals(2, box.segmentsInside(p));
        assertTrue(box.crossedBy(p));
    }

    @Test
    public void crossedByFalseWhenNoSegmentTouches() {
        final AreaBox box = AreaBox.of(area());
        assertFalse(box.crossedBy(plan(seg(50.0, 2.0, 51.0, 3.0))));
        assertEquals(0, box.segmentsInside(plan(seg(50.0, 2.0, 51.0, 3.0))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvertedLatitude() {
        new AreaBox(2.0, 1.0, 0.0, 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvertedLongitude() {
        new AreaBox(0.0, 1.0, 2.0, 1.0);
    }
}
