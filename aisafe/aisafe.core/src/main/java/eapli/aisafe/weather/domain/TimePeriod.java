package eapli.aisafe.weather.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class TimePeriod implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime endTime;

    public TimePeriod(final LocalDateTime startTime, final LocalDateTime endTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time must not be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time must not be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    protected TimePeriod() {
        // for ORM
    }

    public LocalDateTime startTime() {
        return this.startTime;
    }

    public LocalDateTime endTime() {
        return this.endTime;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimePeriod)) {
            return false;
        }
        final TimePeriod that = (TimePeriod) o;
        return this.startTime.equals(that.startTime) && this.endTime.equals(that.endTime);
    }

    @Override
    public int hashCode() {
        return 31 * startTime.hashCode() + endTime.hashCode();
    }

    @Override
    public String toString() {
        return startTime + " → " + endTime;
    }
}
