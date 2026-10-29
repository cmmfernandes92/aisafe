package eapli.aisafe.persistence.impl.inmemory;

import java.util.Optional;

import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryMakerRepository extends InMemoryDomainRepository<Maker, MakerName>
        implements MakerRepository {

    @Override
    public Optional<Maker> findByName(final MakerName name) {
        return matchOne(e -> e.identity().equals(name));
    }
}
