package eapli.aisafe.fleetmanagement.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class BusinessSeats implements ValueObject {

    private static final long serialVersionUID = 1L;

    private int count;

    public BusinessSeats(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Business seat count must not be negative");
        }
        this.count = count;
    }

    protected BusinessSeats() {
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
        if (!(o instanceof BusinessSeats)) {
            return false;
        }
        return this.count == ((BusinessSeats) o).count;
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
