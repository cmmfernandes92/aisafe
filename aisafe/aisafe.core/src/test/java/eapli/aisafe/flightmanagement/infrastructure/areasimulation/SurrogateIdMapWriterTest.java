package eapli.aisafe.flightmanagement.infrastructure.areasimulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eapli.aisafe.flightmanagement.domain.FlightId;
import eapli.aisafe.flightroutemanagement.domain.RouteName;

public class SurrogateIdMapWriterTest {

    private final SurrogateIdMapWriter writer = new SurrogateIdMapWriter();

    @Test
    public void ensureCsvCarriesRouteAndDepartureDateSortedById() {
        // identity is route + departure date (FlightId), not a bare designator —
        // this is exactly what the US111 assembler resolves a Flight by.
        final Map<Integer, FlightId> idMap = new LinkedHashMap<>();
        idMap.put(2, new FlightId(RouteName.valueOf("FR456"), LocalDate.of(2026, 6, 12)));
        idMap.put(1, new FlightId(RouteName.valueOf("TP123"), LocalDate.of(2026, 6, 12)));

        final List<String> lines = writer.toCsv(idMap).lines().toList();

        assertEquals("surrogate_id,route,departure_date", lines.get(0));
        assertEquals("1,TP123,2026-06-12", lines.get(1)); // sorted by surrogate id
        assertEquals("2,FR456,2026-06-12", lines.get(2));
    }

    @Test
    public void ensureWriteCreatesFlightsCsvInRunDir() throws IOException {
        final Path runDir = Files.createTempDirectory("aisafe-idmap");
        try {
            final Map<Integer, FlightId> idMap = Map.of(
                    1, new FlightId(RouteName.valueOf("BA789"), LocalDate.of(2026, 6, 12)));

            final Path file = writer.write(idMap, runDir);

            assertEquals(SurrogateIdMapWriter.FILE_NAME, file.getFileName().toString());
            assertTrue(Files.exists(file));
            assertTrue(Files.readString(file).contains("1,BA789,2026-06-12"));
        } finally {
            deleteRecursively(runDir);
        }
    }

    private static void deleteRecursively(final Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (final IOException ignored) {
                    // best-effort temp cleanup
                }
            });
        }
    }
}