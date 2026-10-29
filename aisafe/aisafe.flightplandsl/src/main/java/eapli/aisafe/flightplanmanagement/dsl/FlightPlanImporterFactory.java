package eapli.aisafe.flightplanmanagement.dsl;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Maps file extensions to {@link FlightPlanImporter} implementations.
 *
 * <p>Add a new format by placing a new entry in {@link #REGISTRY}.
 * Currently supported: {@code .flight} (Core Flight DSL, US083/US120).
 */
public final class FlightPlanImporterFactory {

    private static final Map<String, Supplier<FlightPlanImporter>> REGISTRY;

    static {
        REGISTRY = new LinkedHashMap<>();
        REGISTRY.put("flight", FlightDslCompiler::new);
    }

    private FlightPlanImporterFactory() { }

    /**
     * Returns an importer for the given file, selected by its extension.
     * Returns {@link Optional#empty()} when no importer is registered for that extension.
     */
    public static Optional<FlightPlanImporter> forPath(final Path path) {
        final String ext = extension(path);
        final Supplier<FlightPlanImporter> supplier = REGISTRY.get(ext);
        return Optional.ofNullable(supplier).map(Supplier::get);
    }

    /** Returns the set of registered file extensions (without the leading dot). */
    public static Set<String> supportedExtensions() {
        return REGISTRY.keySet();
    }

    private static String extension(final Path path) {
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
