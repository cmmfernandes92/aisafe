package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import eapli.aisafe.Application;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.aisafe.makermanagement.domain.MakerName;
import eapli.aisafe.makermanagement.repositories.MakerRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaMakerRepository extends JpaAutoTxRepository<Maker, MakerName, MakerName>
        implements MakerRepository {

    public JpaMakerRepository(final TransactionalContext autoTx) {
        super(autoTx, "name");
    }

    public JpaMakerRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "name");
    }

    @Override
    public Optional<Maker> findByName(final MakerName name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        return matchOne("e.name=:name", params);
    }
}
