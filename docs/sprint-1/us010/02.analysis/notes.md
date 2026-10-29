# Analysis Notes — US010

This document records the domain doubts raised during analysis, the clarifications obtained from the Product Owner (PO), and the modelling decisions taken as a result. It serves as a traceability record for the choices reflected in the Domain Model.

---

## 1. Maker Hierarchy — Abstract or Concrete?

**Doubt:** Should `Maker` be modelled as an abstract root with `AircraftMaker` and `EngineMaker` as concrete specializations, or as a concrete class that can also be specialized?

**Clarification (PO, 22/03/2026):** A single real-world manufacturer can produce both aircraft and engines. A manufacturer does not need to be exclusively one or the other.

**Decision:** `Maker` is modelled as a **concrete aggregate root**. `AircraftMaker` and `EngineMaker` are specializations, but a `Maker` can exist without being either — representing a manufacturer of both. Modelling it as abstract would incorrectly prevent this.

---

## 2. Engine Model Certification — Same Manufacturer Required?

**Doubt:** Must the engine models certified for an aircraft model belong to the same manufacturer as the aircraft model?

**Clarification (PO, 22/03/2026):** Engines from different manufacturers are allowed to be certified for an aircraft model.

**Decision:** The `AircraftModel` → `EngineModel` certification association imposes **motorization type compatibility** (the engine's motorization type must match the aircraft's motorization type), but does **not** restrict manufacturer. This is reflected as an invariant in the `AircraftModel` aggregate.

---

## 3. CabinConfiguration — Belongs to AircraftModel or Aircraft?

**Doubt:** `CabinConfiguration` describes seat layout. Should it belong to the `AircraftModel` (as a class-level definition) or to `Aircraft` (as a per-instance configuration)?

**Decision:** `CabinConfiguration` belongs to **`Aircraft`**, not `AircraftModel`. The seat layout is a property of a specific physical aircraft, which may be reconfigured independently of its model. The `AircraftModel` defines maximum capacity as an invariant constraint; the actual cabin layout is defined per aircraft instance. `CabinConfiguration` is modelled as a **value object** inside the `Aircraft` aggregate.

---

## 4. FlightPlan — Separate Aggregate or Inside Flight?

**Doubt:** Should `FlightPlan` be its own aggregate root, or should it be an entity inside the `Flight` aggregate?

**Clarification (PO, 02/04/2026):** A `Flight` has one or more `FlightPlan`s. If a plan is rejected during validation, the pilot submits a new one. Only one may be validated at a time. US080 creates a Flight *including* its first FlightPlan.

**Clarification (PO, 25/03/2026):** At most one FlightPlan per Flight may have status VALID at any time.

**Decision:** `FlightPlan` is modelled as an **entity inside the `Flight` aggregate**. The "only one VALID FlightPlan at a time" invariant must be enforced transactionally within a single boundary. If `FlightPlan` were a separate aggregate, this rule would require eventual consistency, risking two simultaneously valid plans. Additionally, nothing in the system references `FlightPlan` directly — all access goes through `Flight`. Three design alternatives were considered and documented in `design/flight-aggregate-design-alternatives.puml`.

---

## 5. Regular vs Charter Flights

**Doubt:** How is the distinction between regular and charter flights represented in the model?

**Decision:** The distinction is captured through the **`Departure` hierarchy**:
- `RegularDeparture` — one or more `WeekdaySchedule` entries, each with one or more `DepartureTime` values.
- `CharterDeparture` — a single `departureDate` and `departureTime`.

The flight type is determined by which `Departure` subclass is used, not by a separate type field. Both are modelled as **value objects**.

---

## 6. FlightRoutes — Global or Company-Scoped?

**Doubt:** Are flight routes shared across companies, or does each company own its own routes?

**Clarification (PO, 22/03/2026):** Routes are company-owned and not shared as a global network.

**Decision:** `FlightRoute` has a direct association to `AirTransportCompany`. Route uniqueness is scoped to the system but identity is tied to the owning company.

---

## 7. Weather Data — Granularity Within an Air Control Area

**Doubt:** Is weather data uniform across an entire air control area, or can it vary within the same area?

**Clarification (PO, 19/03/2026):** Weather conditions are not uniform across an entire air control area. They can vary within the same area.

**Decision:** A single `AirControlArea` may have multiple `WeatherData` records. The uniqueness invariant is scoped to (area + date + source) to prevent duplicates while allowing the necessary granularity.

---

## 8. SimulationRun — Scope of Java vs SCOMP Module

**Doubt:** What is the Java system responsible for in the simulation, and what belongs to the external C module (SCOMP)?

**Clarification (PO, 27/03/2026):** The core Java system is not responsible for flight simulation internals. It only sends configuration to the C module (SCOMP) and receives a report back.

**Clarification (PO, 26/03/2026):** `PerformanceSettings` is intentionally underspecified for Sprint 1 and will be refined in Sprints 2–3 as simulation requirements become clearer.

**Decision:** The `SimulationRun` aggregate owns the full lifecycle of a simulation (configuration, status, result, violations) on the Java side. `PerformanceSettings` is included as a value object with minimal definition for now.

---

## 9. Wind Direction and Wind Speed — Duplicated in WeatherData and FlightSegment?

**Observation:** Both `WeatherData` and `FlightSegment` have wind-related value objects (`WeatherWindDirection`/`WeatherWindSpeed` vs `WindDirection`/`WindSpeed`). These are distinct classes in the model.

**Decision:** They are kept as **separate value objects** in their respective aggregates. The wind data in `FlightSegment` represents the wind conditions planned for that specific segment in a flight plan. The wind data in `WeatherData` represents observed or forecast weather for a control area. They serve different domain purposes and should not be merged.

---

## 10. Pilot and CompanyCollaborator — Relationship to User

**Doubt:** Should `Pilot` and `CompanyCollaborator` directly store user attributes, or reference the `User` aggregate?

**Decision:** Both `Pilot` and `CompanyCollaborator` reference `User` **by identity**. They represent domain-specific roles derived from a system user. The user's identity and credentials are managed by the `User` aggregate; the role-specific data (certifications, position, company association) is managed by the respective aggregate.