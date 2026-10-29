package eapli.aisafe.flightplanmanagement.dsl.parsing;

import java.util.Set;
import java.util.TreeSet;

public record ParseStats(int flights, int legs, int segments, Set<String> airportsReferenced) {

    public ParseStats {
        airportsReferenced = Set.copyOf(new TreeSet<>(airportsReferenced));
    }
}
