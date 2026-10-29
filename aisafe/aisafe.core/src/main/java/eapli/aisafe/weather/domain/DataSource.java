package eapli.aisafe.weather.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class DataSource implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Column(name = "data_source", nullable = false)
    private String value;

    public DataSource(final String value) {
        if (StringPredicates.isNullOrEmpty(value) || value.isBlank()) {
            throw new IllegalArgumentException("Data source must not be null or blank");
        }
        this.value = value.strip();
    }

    protected DataSource() {
        // for ORM
    }

    public String value() {
        return this.value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataSource)) {
            return false;
        }
        return this.value.equals(((DataSource) o).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }
}
