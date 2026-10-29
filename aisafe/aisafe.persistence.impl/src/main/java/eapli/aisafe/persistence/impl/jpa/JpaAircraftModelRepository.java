package eapli.aisafe.persistence.impl.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.TypedQuery;

import eapli.aisafe.Application;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModelName;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.makermanagement.domain.Maker;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaAircraftModelRepository extends JpaAutoTxRepository<AircraftModel, Long, Long>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaAircraftModelRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Optional<AircraftModel> findByNameAndMaker(final AircraftModelName name, final Maker maker) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("maker", maker);
        return matchOne("e.modelName=:name AND e.maker=:maker", params);
    }

    @Override
    public boolean existsByNameAndMaker(final AircraftModelName name, final Maker maker) {
        final TypedQuery<Long> q = createQuery(
                "SELECT COUNT(e) FROM AircraftModel e WHERE e.modelName=:name AND e.maker=:maker", Long.class);
        q.setParameter("name", name);
        q.setParameter("maker", maker);
        return q.getSingleResult() > 0;
    }
}
