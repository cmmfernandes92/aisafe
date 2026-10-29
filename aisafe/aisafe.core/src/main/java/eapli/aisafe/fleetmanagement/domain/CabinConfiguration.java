package eapli.aisafe.fleetmanagement.domain;

import java.util.Map;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class CabinConfiguration implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Embedded
    @AttributeOverride(name = "count", column = @Column(name = "cabin_economy_seats", nullable = false))
    private EconomySeats economySeats;

    @Embedded
    @AttributeOverride(name = "count", column = @Column(name = "cabin_business_seats", nullable = false))
    private BusinessSeats businessSeats;

    @Embedded
    @AttributeOverride(name = "count", column = @Column(name = "cabin_first_class_seats", nullable = false))
    private FirstClassSeats firstClassSeats;

    public CabinConfiguration(final Map<SeatClass, Integer> seats) {
        if (seats == null || seats.isEmpty()) {
            throw new IllegalArgumentException("Cabin configuration must include at least one seat class");
        }
        for (final Map.Entry<SeatClass, Integer> entry : seats.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException(
                        "Seat count for " + entry.getKey() + " must not be negative");
            }
        }
        this.economySeats = new EconomySeats(seats.getOrDefault(SeatClass.ECONOMY, 0));
        this.businessSeats = new BusinessSeats(seats.getOrDefault(SeatClass.BUSINESS, 0));
        this.firstClassSeats = new FirstClassSeats(seats.getOrDefault(SeatClass.FIRST, 0));
    }

    protected CabinConfiguration() {
        // for ORM
    }

    public int totalSeats() {
        return economySeats.count() + businessSeats.count() + firstClassSeats.count();
    }

    public int economySeats() {
        return economySeats.count();
    }

    public int businessSeats() {
        return businessSeats.count();
    }

    public int firstClassSeats() {
        return firstClassSeats.count();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CabinConfiguration)) {
            return false;
        }
        final CabinConfiguration that = (CabinConfiguration) o;
        return economySeats.equals(that.economySeats)
                && businessSeats.equals(that.businessSeats)
                && firstClassSeats.equals(that.firstClassSeats);
    }

    @Override
    public int hashCode() {
        return totalSeats();
    }

    @Override
    public String toString() {
        return "Economy: " + economySeats.count()
                + ", Business: " + businessSeats.count()
                + ", First: " + firstClassSeats.count();
    }
}
