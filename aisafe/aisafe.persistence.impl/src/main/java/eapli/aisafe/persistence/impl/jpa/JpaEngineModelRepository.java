package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.TypedQuery;

import eapli.aisafe.Application;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.domain.EngineModelName;
import eapli.aisafe.enginemodelmanagement.domain.MotorizationType;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaEngineModelRepository extends JpaAutoTxRepository<EngineModel, Long, Long>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaEngineModelRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Optional<EngineModel> findByNameAndMaker(final EngineModelName name, final Maker maker) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("maker", maker);
        return matchOne("e.engineModelName=:name AND e.maker=:maker", params);
    }

    @Override
    public boolean existsByNameAndMaker(final EngineModelName name, final Maker maker) {
        final TypedQuery<Long> q = createQuery(
                "SELECT COUNT(e) FROM EngineModel e WHERE e.engineModelName=:name AND e.maker=:maker", Long.class);
        q.setParameter("name", name);
        q.setParameter("maker", maker);
        return q.getSingleResult() > 0;
    }

    @Override
    public Iterable<EngineModel> findByMotorizationType(final MotorizationType motorizationType) {
        final Map<String, Object> params = new HashMap<>();
        params.put("type", motorizationType);
        return match("e.motorizationType=:type", params);
    }
}
