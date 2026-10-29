package eapli.aisafe.persistence.impl.jpa;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import eapli.aisafe.Application;
import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.SimulationRunId;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

class JpaSimulationRunRepository
        extends JpaAutoTxRepository<SimulationRun, SimulationRunId, SimulationRunId>
        implements SimulationRunRepository {

    public JpaSimulationRunRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaSimulationRunRepository(final String puname) {
        super(puname, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Iterable<SimulationRun> findByAreaCode(final AreaCode areaCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode);
        return match("e.areaCode = :areaCode", params);
    }

    @Override
    public Iterable<SimulationRun> findInPeriod(final LocalDateTime startInclusive,
            final LocalDateTime endExclusive) {
        final Map<String, Object> params = new HashMap<>();
        params.put("start", startInclusive);
        params.put("end", endExclusive);
        return match("e.runTimestamp >= :start AND e.runTimestamp < :end", params);
    }
}