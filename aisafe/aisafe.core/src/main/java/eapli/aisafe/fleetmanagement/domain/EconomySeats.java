package eapli.aisafe.fleetmanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class EconomySeats implements ValueObject {

    private static final long serialVersionUID = 1L;

    private int count;

    public EconomySeats(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Economy seat count must not be negative");
        }
        this.count = count;
    }

    protected EconomySeats() {
        // for ORM
    }

    public int count() {
        return this.count;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EconomySeats)) {
            return false;
        }
        return this.count == ((EconomySeats) o).count;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.count);
    }

    @Override
    public String toString() {
        return String.valueOf(this.count);
    }
}
