package eapli.aisafe.reporting.domain;

import java.io.Serializable;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Value object that identifies a calendar month (year + month).
 * Invariants: year >= 2000, month 1-12.
 */
public final class MonthlyReportPeriod implements Serializable, Comparable<MonthlyReportPeriod> {

    private static final int MIN_YEAR = 2000;

    private final int year;
    private final int month;

    public MonthlyReportPeriod(int year, int month) {
        if (year < MIN_YEAR) throw new IllegalArgumentException("year must be >= " + MIN_YEAR);
        if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1-12");
        this.year  = year;
        this.month = month;
    }

    public static MonthlyReportPeriod of(YearMonth ym) {
        return new MonthlyReportPeriod(ym.getYear(), ym.getMonthValue());
    }

    public int year()  { return year; }
    public int month() { return month; }

    public String label() {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    public YearMonth toYearMonth() { return YearMonth.of(year, month); }

    public MonthlyReportPeriod previous() {
        return of(toYearMonth().minusMonths(1));
    }

    @Override public int compareTo(MonthlyReportPeriod o) {
        int c = Integer.compare(this.year, o.year);
        return c != 0 ? c : Integer.compare(this.month, o.month);
    }

    @Override public boolean equals(Object o) {
        return o instanceof MonthlyReportPeriod p && year == p.year && month == p.month;
    }
    @Override public int hashCode() { return Objects.hash(year, month); }
    @Override public String toString() { return label(); }
}
