package eapli.aisafe.flightmanagement.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import eapli.aisafe.weather.domain.WeatherDataId;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.util.Map;
import java.util.Collections;


/**
 * A flight plan owned by a {@link Flight}. It is an entity <em>inside</em> the
 * Flight aggregate, not an aggregate root: it carries no own {@code @Version},
 * only a local generated id.
 *
 * <p>US080 creates it empty in {@link FlightPlanStatus#DRAFT} (I1) with no DSL
 * body; US121 supersedes {@code rawDslContent} (recording {@code importedAt} /
 * {@code importedBy}) and promotes it to {@code VALIDATED}; US085 promotes it to
 * {@code TESTED}.</p>
 */


@Entity
public class FlightPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlightPlanStatus status;

    @Lob
    @Column(name = "raw_dsl_content")
    private String rawDslContent;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "imported_by")
    private String importedBy;


    @jakarta.persistence.ElementCollection
    private java.util.List<WeatherAssignment> weatherAssignments = new java.util.ArrayList<>();

    /**
     * Creates an empty plan in {@code DRAFT} (I1). Package-private: a plan only
     * exists inside a {@link Flight}. Also used by the ORM.
     */
    protected  FlightPlan() {
        this.status = FlightPlanStatus.DRAFT;
    }

    public FlightPlanStatus status() {
        return this.status;
    }

    public String rawDslContent() {
        return this.rawDslContent;
    }

    public LocalDateTime importedAt() {
        return this.importedAt;
    }

    public String importedBy() {
        return this.importedBy;
    }

    boolean isValidated() {
        return this.status == FlightPlanStatus.VALIDATED;
    }

    boolean isDraft() {
        return this.status == FlightPlanStatus.DRAFT;
    }

    boolean hasContent() {
        return this.rawDslContent != null && !this.rawDslContent.isBlank();
    }

    /**
     * Replaces the draft's body with imported DSL content and records who
     * imported it and when (US121). Only a {@code DRAFT} plan can be superseded;
     * the {@code DRAFT -> VALIDATED} promotion is driven separately by the owning
     * {@link Flight}.
     */
    void supersede(final String rawDslContent, final String importedBy) {
        if (this.status != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT flight plan can be superseded");
        }
        Preconditions.nonEmpty(rawDslContent, "Flight plan content must not be empty");
        Preconditions.nonEmpty(importedBy, "Importer must be identified");
        this.rawDslContent = rawDslContent;
        this.importedBy = importedBy;
        this.importedAt = LocalDateTime.now();
    }

    /**
     * Promotes a {@code DRAFT} plan to {@code VALIDATED}. Driven by the owning
     * {@link Flight}; the cross-plan "at most one VALIDATED" rule lives there.
     */
    void markValidated() {
        if (this.status != FlightPlanStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT flight plan can be validated");
        }
        this.status = FlightPlanStatus.VALIDATED;
    }

    /**
     * Promotes a {@code VALIDATED} plan to {@code TESTED} (US085). Called by
     * the owning {@link Flight} only after the {@link FlightPlanTestService}
     * reports success; the state is left unchanged on failure.
     */
    void markTested() {
        if (this.status != FlightPlanStatus.VALIDATED) {
            throw new IllegalStateException("Only a VALIDATED flight plan can be marked as tested");
        }
        this.status = FlightPlanStatus.TESTED;
    }

    /**
     * Demotes a {@code VALIDATED} plan back to {@code DRAFT} (US121 re-import):
     * the plan keeps its body but is no longer the validated revision. Driven by
     * the owning {@link Flight} when a newer plan supersedes it.
     */
    void demoteToDraft() {
        if (this.status != FlightPlanStatus.VALIDATED) {
            throw new IllegalStateException("Only a VALIDATED flight plan can be demoted to DRAFT");
        }
        this.status = FlightPlanStatus.DRAFT;
    }

    /**
     * Attaches the pilot's per-segment weather choices (US082). Each entry maps a
     * {@link SegmentKey} {@code (legNumber, segmentIndex)} to the chosen
     * {@link WeatherDataId}. Attachment is <em>latest-wins per cell</em>: a later
     * choice for the same segment supersedes the earlier one (mirroring
     * {@code supersede()}), so re-attaching the same cell is idempotent rather than
     * duplicating rows. Re-attaching weather while {@code TESTED} voids the test (the
     * weather set changed) and demotes the plan back to {@code VALIDATED} — not
     * {@code DRAFT}, since the DSL is unchanged (design-notes §1).
     */
    void attachWeather(final Map<SegmentKey, WeatherDataId> weatherMap) {

        Preconditions.nonNull(weatherMap, "Weather map must not be null");

        if (weatherMap.isEmpty()) {
            throw new IllegalArgumentException("Weather assignments cannot be empty");
        }

        for (final Map.Entry<SegmentKey, WeatherDataId> entry : weatherMap.entrySet()) {

            final SegmentKey key = entry.getKey();
            final WeatherDataId value = entry.getValue();

            Preconditions.nonNull(key, "Segment key must not be null");
            Preconditions.nonNull(value, "Weather data id must not be null");

            // latest-wins: drop any existing choice for this exact segment before re-adding
            this.weatherAssignments.removeIf(a ->
                    a.legNumber() == key.legNumber() && a.segmentIndex() == key.segmentIndex());
            this.weatherAssignments.add(
                    new WeatherAssignment(
                            key.legNumber(),
                            key.segmentIndex(),
                            value.toString()
                    )
            );
        }

        if (this.status == FlightPlanStatus.TESTED) {
            this.status = FlightPlanStatus.VALIDATED;
        }
    }

    /**
     * The pilot's weather choices keyed by segment ordinal — the lookup table the
     * US085 wind resolution reads (left-join: attached forecast overrides the DSL
     * baseline wind, DSL wind is the fallback where no choice exists).
     */
    public Map<SegmentKey, WeatherDataId> weatherAssignmentsBySegment() {
        final Map<SegmentKey, WeatherDataId> out = new java.util.HashMap<>();
        for (final WeatherAssignment a : this.weatherAssignments) {
            out.put(new SegmentKey(a.legNumber(), a.segmentIndex()),
                    WeatherDataId.valueOf(a.weatherDataId()));
        }
        return out;
    }

    //  Métodos auxiliares (para testes)

    boolean hasWeatherAssigned() {
        return !this.weatherAssignments.isEmpty();
    }

    java.util.List<WeatherAssignment> weatherAssignments() {
        return Collections.unmodifiableList(this.weatherAssignments);
    }

}

