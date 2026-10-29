package eapli.aisafe.makermanagement.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;

@Entity
public class Maker implements AggregateRoot<MakerName> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private MakerName name;

    public Maker(final MakerName name) {
        Preconditions.nonNull(name, "Maker name must not be null");
        this.name = name;
    }

    protected Maker() {
        // for ORM
    }

    public static Maker valueOf(final String name) {
        return new Maker(MakerName.valueOf(name));
    }

    @Override
    public MakerName identity() {
        return this.name;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return this.name.value();
    }
}