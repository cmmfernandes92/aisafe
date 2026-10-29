package eapli.aisafe.aircraftmodelmanagement.application;

import eapli.aisafe.aircraftmodelmanagement.domain.AircraftModel;
import eapli.aisafe.aircraftmodelmanagement.repositories.AircraftModelRepository;
import eapli.aisafe.enginemodelmanagement.domain.EngineModel;
import eapli.aisafe.enginemodelmanagement.repositories.EngineModelRepository;
import eapli.aisafe.fleetmanagement.domain.Aircraft;
import eapli.aisafe.fleetmanagement.domain.OperationalStatus;
import eapli.aisafe.fleetmanagement.repositories.AircraftRepository;
import eapli.aisafe.usermanagement.domain.AisafeRoles;
import eapli.framework.application.ApplicationService;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

/**
 * Application service for US058 — Remove an engine model from an aircraft model.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>The engine must be currently certified for the aircraft model</li>
 *   <li>A2 — the aircraft model must retain at least one certified engine
 *       (enforced inside the domain by {@code AircraftModel.removeCertifiedEngine()})</li>
 *   <li>AC2 — removal fails if any active aircraft in the fleet is an instance
 *       of this aircraft model. Because the {@code Aircraft} aggregate does not record
 *       which specific engine it uses (only the model), the presence of any active aircraft
 *       of this model is treated as an "in-use" condition for any of its certified engines.</li>
 * </ul>
 *
 * <p>Authorization: BACKOFFICE_OPERATOR only.
 */
@ApplicationService
public class RemoveEngineModelFromAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;
    private final AircraftRepository aircraftRepository;

    public RemoveEngineModelFromAircraftModelController(
            final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository,
            final AircraftRepository aircraftRepository) {
        Preconditions.noneNull(aircraftModelRepository, engineModelRepository, aircraftRepository);
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository   = engineModelRepository;
        this.aircraftRepository      = aircraftRepository;
    }

    /**
     * Returns all aircraft models so the UI can present a selection list.
     */
    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);
        return aircraftModelRepository.findAll();
    }

    /**
     * Returns all certified engines for a given aircraft model.
     * Used by the UI to let the operator select which engine to remove.
     */
    public Iterable<EngineModel> certifiedEnginesOf(final AircraftModel model) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);
        Preconditions.nonNull(model, "Aircraft model must not be null");
        return model.certifiedEngines();
    }

    /**
     * Removes an engine model from an aircraft model's certified engines list (US058).
     *
     * <p>Two failure modes with distinct error messages (US058.AC4):
     * <ul>
     *   <li>{@code IllegalStateException} — removing would leave zero engines (A2),
     *       or active aircraft of this model exist (AC2)</li>
     *   <li>{@code IllegalArgumentException} — model/engine not found,
     *       or engine not certified for this model</li>
     * </ul>
     *
     * @param aircraftModelId the ID of the aircraft model
     * @param engineModelId   the ID of the engine model to remove
     * @return the updated aircraft model
     * @throws IllegalArgumentException if model or engine not found, or engine not certified
     * @throws IllegalStateException    if removing violates A2 or AC2
     */
    public AircraftModel removeEngine(final Long aircraftModelId, final Long engineModelId) {
        authz.ensureAuthenticatedUserHasAnyOf(AisafeRoles.BACKOFFICE_OPERATOR);

        final AircraftModel model = aircraftModelRepository.ofIdentity(aircraftModelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No aircraft model found with id " + aircraftModelId));

        final EngineModel engine = engineModelRepository.ofIdentity(engineModelId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No engine model found with id " + engineModelId));

        // AC2: cross-aggregate check — fail if any active aircraft uses this model.
        // Aircraft does not record the specific engine used; the certified-engine list
        // on the model is the only record of which engines are valid for that aircraft type.
        // Therefore the presence of any active aircraft of this model is treated as
        // "in use" for any of its certified engines.
        final boolean hasActiveAircraft = hasActiveAircraftForModel(aircraftModelId);
        if (hasActiveAircraft) {
            throw new IllegalStateException(
                    "Cannot remove engine \"" + engine.engineModelName()
                            + "\" — active aircraft in the fleet are instances of this aircraft model. "
                            + "Retire all aircraft of this model before modifying its certified engines.");
        }

        // Domain enforces A2: throws IllegalStateException if this is the last engine
        model.removeCertifiedEngine(engine);

        return aircraftModelRepository.save(model);
    }

    /**
     * Checks whether any active {@link Aircraft} in the fleet references this aircraft model.
     *
     * <p>Note: {@link AircraftRepository} does not expose a query by {@code modelId},
     * so this check iterates {@code findAll()} and filters in memory. For production
     * deployments a dedicated repository method (e.g. {@code existsActiveByModelId(Long)})
     * should be added to push the filter to the database.
     */
    private boolean hasActiveAircraftForModel(final Long aircraftModelId) {
        for (final Aircraft aircraft : aircraftRepository.findAll()) {
            if (aircraft.modelId().equals(aircraftModelId)
                    && aircraft.status() == OperationalStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }
}