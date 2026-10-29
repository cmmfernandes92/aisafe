# Domain Model - Sprint 3 Updates

## Corrections from Sprint 2 feedback

- Tagged `CompanyCollaborator` and `AreaCollaborator` as `<<entity>>`, like the other entities in the model.
- Moved cross-aggregate references so they always start from the aggregate root and never from an inner class (for example, the "certified for" arrow now leaves `Pilot` instead of `PilotCertification`).
- Replaced the dashed dependency between `CabinConfiguration` and `SeatClass` with a plain composition, since the dashed arrow is meant for interface realization.

## Weather aggregate (US042)

US042 made the `WeatherData` aggregate zone-banded by adding the `WeatherZone` and `AltitudeBand` value objects, with `WeatherZone` reusing the existing `Coordinates` value object.

## SimulationRun aggregate (US111)

US111 ingests the simulation report into the existing `SimulationRun` aggregate and forced two deltas:

- Added `FlightExecutionStatus` (`<<entity>>`, attributes `flightDesignator`, `exitCode`) under `SimulationResult` (`SimulationResult *-- "0..*" FlightExecutionStatus`), with a `FlightOutcome` enum (`COMPLETED`, `ABORTED`, `TERMINATED_BY_VIOLATION`, `CRASHED`). The report lists each flight's execution status (US111.2), which the model did not previously capture. The flight is referenced by identity (`flightDesignator`); the cross-aggregate link to `Flight` stays root-to-root from `SimulationRun`.
- Classified safety violations: added a `ViolationKind` enum (`SEPARATION`, `ALTITUDE_CEILING`, `ALTITUDE_FLOOR`, `UNKNOWN`) composed by `SafetyViolation` (`SafetyViolation *-- ViolationKind`), and gave `SafetyViolation` the `horizontalDistance`/`verticalDistance` attributes the report carries (US111.3).
- Replaced the `ViolationPosition` value object with `InvolvedFlight` (`<<value object>>`, attributes `flightDesignator`, `position`, `velocity`): a violation now composes `1..2` `InvolvedFlight` — exactly one for an altitude ceiling/floor bust and two distinct flights for a loss of separation — instead of one position per involved flight. Each `InvolvedFlight` carries its aircraft's position and velocity at the moment of the event.