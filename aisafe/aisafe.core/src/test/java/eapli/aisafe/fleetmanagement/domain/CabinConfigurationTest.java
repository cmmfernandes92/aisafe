package eapli.aisafe.fleetmanagement.domain;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

public class CabinConfigurationTest {

    @Test(expected = IllegalArgumentException.class)
    public void ensureCabinConfigurationRejectsNullMap() {
        new CabinConfiguration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCabinConfigurationRejectsEmptyMap() {
        new CabinConfiguration(Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCabinConfigurationRejectsNegativeEconomyCount() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, -1);
        new CabinConfiguration(seats);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCabinConfigurationRejectsNegativeBusinessCount() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 100);
        seats.put(SeatClass.BUSINESS, -5);
        new CabinConfiguration(seats);
    }

    @Test
    public void ensureTotalSeatsSumsAllClasses() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 150);
        seats.put(SeatClass.BUSINESS, 30);
        seats.put(SeatClass.FIRST, 10);
        final CabinConfiguration config = new CabinConfiguration(seats);
        assertEquals(190, config.totalSeats());
    }

    @Test
    public void ensureMissingClassesDefaultToZero() {
        final Map<SeatClass, Integer> seats = new EnumMap<>(SeatClass.class);
        seats.put(SeatClass.ECONOMY, 180);
        final CabinConfiguration config = new CabinConfiguration(seats);
        assertEquals(180, config.totalSeats());
        assertEquals(0, config.businessSeats());
        assertEquals(0, config.firstClassSeats());
    }
}
