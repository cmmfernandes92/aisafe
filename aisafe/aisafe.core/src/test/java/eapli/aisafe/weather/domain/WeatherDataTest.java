package eapli.aisafe.weather.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.aircontrolareamanagement.domain.Coordinates;

public class WeatherDataTest {

    private static final LocalDateTime START = LocalDateTime.of(2024, 1, 15, 6, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 1, 15, 12, 0);

    private static WeatherZone validZone() {
        return new WeatherZone(
                new Coordinates(38.0, -10.0),
                new Coordinates(40.0, -8.0),
                new AltitudeBand(0.0, 1000.0));
    }

    private WeatherData validWeatherData() {
        return new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                15.0, 1013.0, 8000.0,
                new WeatherWindSpeed(10.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    // ── TimePeriod ───────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureTimePeriodRejectsStartAfterEnd() {
        new TimePeriod(END, START);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTimePeriodRejectsEqualStartAndEnd() {
        new TimePeriod(START, START);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTimePeriodRejectsNullStart() {
        new TimePeriod(null, END);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureTimePeriodRejectsNullEnd() {
        new TimePeriod(START, null);
    }

    @Test
    public void ensureTimePeriodHappyPath() {
        final TimePeriod period = new TimePeriod(START, END);
        assertEquals(START, period.startTime());
        assertEquals(END, period.endTime());
    }

    // ── WeatherWindDirection ─────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureWindDirectionRejectsValueAbove360() {
        new WeatherWindDirection(361.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWindDirectionRejectsNegativeValue() {
        new WeatherWindDirection(-1.0);
    }

    @Test
    public void ensureWindDirectionAcceptsBoundaryValues() {
        final WeatherWindDirection zero = new WeatherWindDirection(0.0);
        final WeatherWindDirection full = new WeatherWindDirection(360.0);
        assertEquals(0.0, zero.value(), 0.0);
        assertEquals(360.0, full.value(), 0.0);
    }

    // ── WeatherWindSpeed ─────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureWindSpeedRejectsNegativeValue() {
        new WeatherWindSpeed(-1.0);
    }

    @Test
    public void ensureWindSpeedAcceptsZero() {
        final WeatherWindSpeed calm = new WeatherWindSpeed(0.0);
        assertEquals(0.0, calm.value(), 0.0);
    }

    // ── WeatherData ──────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullAreaCode() {
        new WeatherData(
                null,
                new TimePeriod(START, END),
                validZone(),
                20.0, 1013.0, 10000.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullTimePeriod() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                null,
                validZone(),
                20.0, 1013.0, 10000.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullZone() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                null,
                20.0, 1013.0, 10000.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    // ── AltitudeBand ─────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureAltitudeBandRejectsUpperNotAboveLower() {
        new AltitudeBand(1000.0, 1000.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureAltitudeBandRejectsNegativeLower() {
        new AltitudeBand(-1.0, 1000.0);
    }

    @Test
    public void ensureAltitudeBandHappyPath() {
        final AltitudeBand band = new AltitudeBand(0.0, 1000.0);
        assertEquals(0.0, band.lowerFeet(), 0.0);
        assertEquals(1000.0, band.upperFeet(), 0.0);
    }

    // ── WeatherZone ──────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherZoneRejectsSwLatitudeNotBelowNe() {
        new WeatherZone(
                new Coordinates(40.0, -10.0),
                new Coordinates(40.0, -8.0),
                new AltitudeBand(0.0, 1000.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherZoneRejectsNullAltitudeBand() {
        new WeatherZone(
                new Coordinates(38.0, -10.0),
                new Coordinates(40.0, -8.0),
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNonPositivePressure() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                20.0, 0.0, 10000.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNegativeVisibility() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                20.0, 1013.0, -1.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullDataSource() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                20.0, 1013.0, 10000.0,
                new WeatherWindSpeed(15.0),
                new WeatherWindDirection(270.0),
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullWindSpeed() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                20.0, 1013.0, 10000.0,
                null,
                new WeatherWindDirection(270.0),
                new DataSource("IPMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherDataRejectsNullWindDirection() {
        new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                20.0, 1013.0, 10000.0,
                new WeatherWindSpeed(15.0),
                null,
                new DataSource("IPMA"));
    }

    @Test
    public void ensureWeatherDataHappyPath() {
        final WeatherData data = validWeatherData();
        assertNotNull(data.identity());
        assertEquals(AreaCode.valueOf("LPPC"), data.areaCode());
        assertEquals(1013.0, data.pressure(), 0.0);
    }

    @Test
    public void ensureWindOnlyRecordIsValidWithAbsentMeasurements() {
        final WeatherData data = new WeatherData(
                AreaCode.valueOf("LPPC"),
                new TimePeriod(START, END),
                validZone(),
                new WeatherWindSpeed(10.0),
                new WeatherWindDirection(270.0),
                new DataSource("Happy Weather"));
        assertNotNull(data.identity());
        assertEquals(null, data.temperature());
        assertEquals(null, data.pressure());
        assertEquals(null, data.visibility());
    }

    @Test
    public void ensureWeatherDataGeneratesUniqueIdentities() {
        final WeatherData d1 = validWeatherData();
        final WeatherData d2 = validWeatherData();
        assertNotNull(d1.identity());
        assertNotNull(d2.identity());
        assertEquals(false, d1.identity().equals(d2.identity()));
    }

    @Test
    public void ensureWeatherDataEqualityUsesIdentitySemantics() {
        final WeatherData data = validWeatherData();
        assertTrue(data.equals(data));
        assertFalse(data.equals(validWeatherData())); // different generated identity
        assertFalse(data.equals(null));
        assertNotNull(Integer.valueOf(data.hashCode()));
    }

    @Test
    public void ensureOrmConstructorsAreInstantiable() {
        // protected no-arg constructors exist for the ORM; exercised here for completeness
        assertNotNull(new WeatherData());
        assertNotNull(new TimePeriod());
        assertNotNull(new WeatherZone());
        assertNotNull(new AltitudeBand());
        assertNotNull(new WeatherWindSpeed());
        assertNotNull(new WeatherWindDirection());
        assertNotNull(new DataSource());
    }

    @Test
    public void ensureWeatherDataExposesAllItsParts() {
        final WeatherData data = validWeatherData();
        assertEquals(AreaCode.valueOf("LPPC"), data.areaCode());
        assertEquals(new TimePeriod(START, END), data.timePeriod());
        assertEquals(validZone(), data.zone());
        assertEquals(Double.valueOf(15.0), data.temperature());
        assertEquals(new WeatherWindSpeed(10.0), data.windSpeed());
        assertEquals(new WeatherWindDirection(270.0), data.windDirection());
        assertEquals(new DataSource("IPMA"), data.dataSource());
        assertNotNull(data.toString());
        assertTrue(data.sameAs(data));
    }

    // ── AltitudeBand equality / representation ───────────────────────────────

    @Test
    public void ensureAltitudeBandEqualityIsValueBased() {
        final AltitudeBand band = new AltitudeBand(0.0, 1000.0);
        assertEquals(band, band);
        assertEquals(new AltitudeBand(0.0, 1000.0), band);
        assertEquals(new AltitudeBand(0.0, 1000.0).hashCode(), band.hashCode());
        assertNotEquals(new AltitudeBand(0.0, 2000.0), band);
        assertNotEquals(new AltitudeBand(1.0, 1000.0), band);
        assertNotEquals(band, null);
        assertNotEquals(band, "not a band");
        assertNotNull(band.toString());
    }

    // ── WeatherZone branches and equality ────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherZoneRejectsNullSouthwest() {
        new WeatherZone(null, new Coordinates(40.0, -8.0), new AltitudeBand(0.0, 1000.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherZoneRejectsNullNortheast() {
        new WeatherZone(new Coordinates(38.0, -10.0), null, new AltitudeBand(0.0, 1000.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureWeatherZoneRejectsSwLongitudeNotBelowNe() {
        new WeatherZone(new Coordinates(38.0, -8.0), new Coordinates(40.0, -8.0),
                new AltitudeBand(0.0, 1000.0));
    }

    @Test
    public void ensureWeatherZoneEqualityIsValueBasedAndExposesParts() {
        final WeatherZone zone = validZone();
        assertEquals(zone, zone);
        assertEquals(validZone(), zone);
        assertEquals(validZone().hashCode(), zone.hashCode());
        assertNotEquals(new WeatherZone(new Coordinates(38.0, -10.0), new Coordinates(40.0, -8.0),
                new AltitudeBand(0.0, 2000.0)), zone);
        assertNotEquals(zone, null);
        assertNotEquals(zone, "not a zone");
        assertEquals(new Coordinates(38.0, -10.0), zone.southwest());
        assertEquals(new Coordinates(40.0, -8.0), zone.northeast());
        assertEquals(new AltitudeBand(0.0, 1000.0), zone.altitudeBand());
        assertNotNull(zone.toString());
    }

    // ── Reused value objects: equality / representation ──────────────────────

    @Test
    public void ensureTimePeriodEqualityIsValueBased() {
        final TimePeriod period = new TimePeriod(START, END);
        assertEquals(period, period);
        assertEquals(new TimePeriod(START, END), period);
        assertEquals(new TimePeriod(START, END).hashCode(), period.hashCode());
        assertNotEquals(new TimePeriod(START, END.plusHours(1)), period);
        assertNotEquals(period, null);
        assertNotEquals(period, "not a period");
        assertNotNull(period.toString());
    }

    @Test
    public void ensureWindSpeedEqualityIsValueBased() {
        final WeatherWindSpeed speed = new WeatherWindSpeed(10.0);
        assertEquals(speed, speed);
        assertEquals(new WeatherWindSpeed(10.0), speed);
        assertEquals(new WeatherWindSpeed(10.0).hashCode(), speed.hashCode());
        assertNotEquals(new WeatherWindSpeed(11.0), speed);
        assertNotEquals(speed, null);
        assertNotEquals(speed, "not a speed");
        assertEquals(10.0, speed.value(), 0.0);
        assertNotNull(speed.toString());
    }

    @Test
    public void ensureWindDirectionEqualityIsValueBased() {
        final WeatherWindDirection dir = new WeatherWindDirection(270.0);
        assertEquals(dir, dir);
        assertEquals(new WeatherWindDirection(270.0), dir);
        assertEquals(new WeatherWindDirection(270.0).hashCode(), dir.hashCode());
        assertNotEquals(new WeatherWindDirection(180.0), dir);
        assertNotEquals(dir, null);
        assertNotEquals(dir, "not a direction");
        assertEquals(270.0, dir.value(), 0.0);
        assertNotNull(dir.toString());
    }

    // ── DataSource ───────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void ensureDataSourceRejectsNull() {
        new DataSource(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDataSourceRejectsEmpty() {
        new DataSource("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureDataSourceRejectsBlank() {
        new DataSource("   ");
    }

    @Test
    public void ensureDataSourceTrimsAndComparesByValue() {
        final DataSource source = new DataSource("  IPMA  ");
        assertEquals("IPMA", source.value());
        assertEquals(new DataSource("IPMA"), source);
        assertEquals(new DataSource("IPMA").hashCode(), source.hashCode());
        assertEquals(source, source);
        assertNotEquals(new DataSource("Happy Weather"), source);
        assertNotEquals(source, null);
        assertNotEquals(source, "not a source");
        assertNotNull(source.toString());
    }

    // ── WeatherDataId ────────────────────────────────────────────────────────

    @Test
    public void ensureWeatherDataIdIsReflexiveAndOrdered() {
        final WeatherDataId id = WeatherDataId.newId();
        final WeatherDataId other = WeatherDataId.newId();
        assertEquals(id, id);
        assertNotEquals(id, other);
        assertNotEquals(id, null);
        assertNotEquals(id, "not an id");
        assertNotNull(id.id());
        assertNotNull(id.toString());
        assertEquals(0, id.compareTo(id));
        assertNotEquals(0, id.compareTo(other));
        assertEquals(id.hashCode(), id.hashCode());
    }
}
