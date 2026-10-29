package eapli.aisafe.reporting.repositories;

import java.time.LocalDateTime;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.SimulationRunId;
import eapli.framework.domain.repositories.DomainRepository;

public interface SimulationRunRepository extends DomainRepository<SimulationRunId, SimulationRun> {

    /**
     * Runs stamped with the given area. Backs US112's monthly aggregation
     * ("violations per area") over the persisted runs.
     */
    Iterable<SimulationRun> findByAreaCode(AreaCode areaCode);

    /**
     * Runs whose {@code runTimestamp} falls in [{@code startInclusive}, {@code endExclusive}).
     * Backs the per-month aggregation of US112's monthly statistics report.
     */
    Iterable<SimulationRun> findInPeriod(LocalDateTime startInclusive, LocalDateTime endExclusive);
}