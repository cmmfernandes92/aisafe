package eapli.aisafe.reporting.domain;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyReportPeriodTest {

    // ---- constructor — valid ----

    @Test
    void constructor_minimumValidYear_createsInstance() {
        final var p = new MonthlyReportPeriod(2000, 1);
        assertEquals(2000, p.year());
        assertEquals(1, p.month());
    }

    @Test
    void constructor_validYearAndMonth_createsInstance() {
        final var p = new MonthlyReportPeriod(2026, 5);
        assertEquals(2026, p.year());
        assertEquals(5, p.month());
    }

    // ---- constructor — invalid year ----

    @Test
    void constructor_yearBeforeMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyReportPeriod(1999, 6));
    }

    // ---- constructor — invalid month ----

    @Test
    void constructor_monthZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyReportPeriod(2026, 0));
    }

    @Test
    void constructor_monthThirteen_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MonthlyReportPeriod(2026, 13));
    }

    // ---- accessors ----

    @Test
    void year_returnsCorrectYear() {
        assertEquals(2024, new MonthlyReportPeriod(2024, 3).year());
    }

    @Test
    void month_returnsCorrectMonth() {
        assertEquals(12, new MonthlyReportPeriod(2024, 12).month());
    }

    // ---- toString / label ----

    @Test
    void toString_returnsHumanReadableLabel() {
        assertEquals("January 2025", new MonthlyReportPeriod(2025, 1).toString());
    }

    // ---- equals ----

    @Test
    void equals_sameYearAndMonth_returnsTrue() {
        assertEquals(new MonthlyReportPeriod(2026, 5), new MonthlyReportPeriod(2026, 5));
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        final var p = new MonthlyReportPeriod(2026, 5);
        assertEquals(p, p);
    }

    @Test
    void equals_differentMonth_returnsFalse() {
        assertNotEquals(new MonthlyReportPeriod(2026, 5), new MonthlyReportPeriod(2026, 6));
    }

    @Test
    void equals_differentYear_returnsFalse() {
        assertNotEquals(new MonthlyReportPeriod(2025, 5), new MonthlyReportPeriod(2026, 5));
    }

    @Test
    void equals_null_returnsFalse() {
        assertFalse(new MonthlyReportPeriod(2026, 5).equals(null));
    }

    @Test
    void equals_differentType_returnsFalse() {
        assertFalse(new MonthlyReportPeriod(2026, 5).equals("2026-05"));
    }

    // ---- hashCode ----

    @Test
    void hashCode_sameValues_returnsSameHash() {
        assertEquals(
                new MonthlyReportPeriod(2026, 5).hashCode(),
                new MonthlyReportPeriod(2026, 5).hashCode());
    }

    // ---- compareTo ----

    @Test
    void compareTo_lesserMonth_returnsNegative() {
        assertTrue(new MonthlyReportPeriod(2026, 4).compareTo(new MonthlyReportPeriod(2026, 5)) < 0);
    }

    @Test
    void compareTo_greaterMonth_returnsPositive() {
        assertTrue(new MonthlyReportPeriod(2026, 6).compareTo(new MonthlyReportPeriod(2026, 5)) > 0);
    }

    @Test
    void compareTo_equalPeriod_returnsZero() {
        assertEquals(0, new MonthlyReportPeriod(2026, 5).compareTo(new MonthlyReportPeriod(2026, 5)));
    }

    @Test
    void compareTo_earlierYear_returnsNegative() {
        assertTrue(new MonthlyReportPeriod(2025, 12).compareTo(new MonthlyReportPeriod(2026, 1)) < 0);
    }

    @Test
    void compareTo_laterYear_returnsPositive() {
        assertTrue(new MonthlyReportPeriod(2027, 1).compareTo(new MonthlyReportPeriod(2026, 12)) > 0);
    }

    // ---- of(YearMonth) ----

    @Test
    void of_yearMonth_createsCorrectPeriod() {
        final var p = MonthlyReportPeriod.of(YearMonth.of(2026, 5));
        assertEquals(2026, p.year());
        assertEquals(5, p.month());
    }
}