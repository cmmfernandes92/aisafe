package eapli.aisafe.makermanagement.repositories;

import java.util.Optional;

import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.framework.domain.repositories.DomainRepository;

public interface MakerRepository extends DomainRepository<MakerName, Maker> {

    Optional<Maker> findByName(MakerName name);
}
