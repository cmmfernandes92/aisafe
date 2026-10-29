package eapli.aisafe.persistence.impl.inmemory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import eapli.aisafe.aircontrolareamanagement.domain.AreaCode;
import eapli.aisafe.reporting.domain.SimulationRun;
import eapli.aisafe.reporting.domain.SimulationRunId;
import eapli.aisafe.reporting.repositories.SimulationRunRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemorySimulationRunRepository
        extends InMemoryDomainRepository<SimulationRun, SimulationRunId>
        implements SimulationRunRepository {

    @Override
    public Iterable<SimulationRun> findByAreaCode(final AreaCode areaCode) {
        final List<SimulationRun> result = new ArrayList<>();
        for (final SimulationRun run : findAll()) {
            if (run.areaCode().equals(areaCode)) {
                result.add(run);
            }
        }
        return result;
    }

    @Override
    public Iterable<SimulationRun> findInPeriod(final LocalDateTime startInclusive,
            final LocalDateTime endExclusive) {
        final List<SimulationRun> result = new ArrayList<>();
        for (final SimulationRun run : findAll()) {
            final LocalDateTime ts = run.runTimestamp();
            if (!ts.isBefore(startInclusive) && ts.isBefore(endExclusive)) {
                result.add(run);
            }
        }
        return result;
    }
}