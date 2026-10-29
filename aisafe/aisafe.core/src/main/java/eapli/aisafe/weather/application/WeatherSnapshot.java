package eapli.aisafe.weather.application;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.weather.domain.DataSource;
import eapli.aisafe.weather.domain.TimePeriod;

/**
 * Read-side weather import snapshot, derived from persisted weather rows.
 *
 * <p>There is no persisted import identifier yet, so the batch key is the
 * heuristic pair {@code (dataSource, period)}. Re-importing the same provider and
 * period intentionally merges into this snapshot; a future import-id/imported-at
 * on WeatherData would make this identity explicit.</p>
 */
public record WeatherSnapshot(DataSource source, TimePeriod period, int recordCount, Set<AreaCode> areas) {

    public WeatherSnapshot {
        if (source == null) {
            throw new IllegalArgumentException("Data source must not be null");
        }
        if (period == null) {
            throw new IllegalArgumentException("Time period must not be null");
        }
        if (recordCount < 0) {
            throw new IllegalArgumentException("Record count must not be negative");
        }
        if (areas == null) {
            throw new IllegalArgumentException("Areas must not be null");
        }
        areas = Collections.unmodifiableSet(new LinkedHashSet<>(new TreeSet<>(areas)));
    }
}
