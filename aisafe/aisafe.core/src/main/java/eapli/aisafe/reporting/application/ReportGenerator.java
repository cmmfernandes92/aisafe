package eapli.aisafe.reporting.application;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Foundational Strategy interface for all report generators in the system.
 * US112 states this work must be foundational for future report types.
 * @param <D> the report data type
 */
public interface ReportGenerator<D> {
    void generate(D data, Path outputPath) throws IOException;
    String fileExtension();
}
