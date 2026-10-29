package eapli.aisafe.weather.application.importing;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of a bulk import: how many records were persisted, and the rows that
 * were skipped (with reasons). A DTO returned by the controller to the UI so the
 * domain is not exposed to the presentation layer.
 */
public final class ImportReport {

    private final int imported;
    private final List<RowError> errors;

    public ImportReport(final int imported, final List<RowError> errors) {
        this.imported = imported;
        this.errors = List.copyOf(errors);
    }

    public int imported() {
        return this.imported;
    }

    public int skipped() {
        return this.errors.size();
    }

    public List<RowError> errors() {
        return Collections.unmodifiableList(this.errors);
    }

    @Override
    public String toString() {
        return "Imported " + imported + ", skipped " + skipped();
    }
}