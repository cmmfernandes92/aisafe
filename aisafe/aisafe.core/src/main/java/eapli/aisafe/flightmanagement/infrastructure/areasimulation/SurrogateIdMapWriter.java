package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.framework.validations.Preconditions;

/**
 * Writes the surrogate-id sidecar ({@code flights.csv}) next to the SCOMP
 * {@code scenario.json}/{@code report.txt} in a run folder. This is the egress
 * half of the US111 integration contract (Option A): SCOMP's report identifies
 * flights by an opaque integer surrogate, so the egress records the reverse
 * mapping on disk and the US111 ingress joins {@code report.txt}'s
 * {@code flight_id} back to a domain {@link FlightId}.
 *
 * <p>A flight's identity is {@code route + departure_date} (see {@link FlightId}),
 * so each row carries both — not a bare designator — which is exactly what the
 * US111 assembler resolves a {@code Flight} by.
 */
public class SurrogateIdMapWriter {

    /** Sidecar file name, sibling of {@code scenario.json} in the run folder. */
    public static final String FILE_NAME = "flights.csv";

    private static final String HEADER = "surrogate_id,route,departure_date";

    /**
     * Renders the id map as CSV: a header line followed by one row per flight,
     * sorted by surrogate id for stable, inspectable output.
     */
    public String toCsv(final Map<Integer, FlightId> idMap) {
        Preconditions.nonNull(idMap);
        final StringBuilder sb = new StringBuilder(HEADER).append('\n');
        idMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    final FlightId flightId = entry.getValue();
                    sb.append(entry.getKey()).append(',')
                            .append(flightId.routeName().value()).append(',')
                            .append(flightId.departureDate()) // ISO-8601, e.g. 2026-06-12
                            .append('\n');
                });
        return sb.toString();
    }

    /**
     * Writes {@code flights.csv} into {@code dir} and returns its path.
     */
    public Path write(final Map<Integer, FlightId> idMap, final Path dir) throws IOException {
        Preconditions.noneNull(idMap, dir);
        final Path file = dir.resolve(FILE_NAME);
        Files.writeString(file, toCsv(idMap), StandardCharsets.UTF_8);
        return file;
    }
}