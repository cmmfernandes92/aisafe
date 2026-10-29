# PO Clarifications - Sprint 3

## 1. Weather Data

An **example CSV file with weather data will be provided**. Teams may need to create **additional CSV files for testing purposes**. (21/05/2026)

Bulk weather data import may rely on either **files or API calls to external systems**. Support for the **provided file format is mandatory**. (21/05/2026)

Pilots **do not enter weather data manually into a flight plan**. The pilot **selects from the weather forecasts already available in the system**. Where a flight covers multiple air control areas or time windows, the pilot may **select more than one forecast**. If **no forecast is available** for the relevant area and time, the pilot **cannot make a selection**. This is aligned with US043 and reflects US042's premise that **multiple weather data providers may produce multiple forecasts for the same area at the same time**. (21/05/2026, 25/05/2026)

## 2. Flight Plans (US080, US081, US082)

US080 **does not require the system to parse the flight plan line by line**. The flight plan content may be **accepted as a string or read from a text file**, and **no validation is required at this stage**. (21/05/2026)

US081 differs from US080 in that it **requires validation of the flight plan description**: lexical, syntactic, and semantic. **Only valid flight plans may be imported**. (23/05/2026)

In US082 the pilot **does not manually load weather data**; the pilot **selects from forecasts already registered in the system**, and the chosen forecast(s) **enrich the flight plan before testing**. (25/05/2026)

Fuel quantity belongs in the flight plan description (per leg), not duplicated on the Flight entity. It is used only for simulation. (02/06/2026)

The departure time of the first leg, although already present in the flight plan DSL, should also be stored on the Flight entity since it is frequently accessed. The duplication is accepted, but US085 must verify that the two values remain consistent. (02/06/2026)

The aircraft used for a flight must belong to the same company that owns the route. Code sharing is out of scope. (30/05/2026)

For the MVP, not all pilots of a company may submit flight plans for the same flight; creating plans for a given flight is exclusive, not open to every company pilot. (03/06/2026)

**All flight plans belonging to the same flight must use the same aircraft**. A flight cannot have plans that specify different aircraft. (03/06/2026)

The DSL does not create flight plans; it is a language for describing them. (05/06/2026)

## 3. Routes (US074)

Deactivating a route **does not mean removing it from the database**. The route must be **retained with a deactivation indicator** (e.g. a deactivation date) so that **pending and historical flights remain coherent**. From the deactivation date onwards, **no new flights may be created on that route**. (25/05/2026)

## 4. Pilots

Adding a pilot to an Air Transport Company is the **responsibility of the Air Transport Company Collaborator via US075**, not of the Backoffice Operator via US061. The two user stories serve **distinct roles** and are **not interchangeable**. (21/05/2026)

A pilot has a **list of aircraft models he or she is certified to fly**. Real-world details such as licence categories, classes, flight hours, or aircraft-specific ratings are **not in scope**. Equally, assuming **any pilot can fly any aircraft is outside the requirements**. (23/05/2026)

Pilots do not have a licence number. It is not part of the requirements and should not be added to the domain model. (03/06/2026)

A pilot must be certified for at least one aircraft model. There is no upper limit on the number of certifications, and the removal of certifications is out of scope. (03/06/2026)

A pilot works for exactly one Air Transport Company. The N:M scenario (a pilot freelancing across multiple companies) is not supported; if it were, US075 would not bind the act of adding a pilot to a specific company, but it does. (03/06/2026)

Pilot attributes are limited to what the requirements document specifies. Calculated values such as flight hours must not be stored as pilot attributes. (03/06/2026)

## 5. Users

Skills assessment applies to all users, not only pilots. Assessment dates are always in the past — assessments cannot be scheduled into the future. Registering a new user requires at least one prior skills assessment on record. (03/06/2026)

## 6. Air Control Areas

Weather information is **not used to define or create air control areas**. ACAs are **created independently in the system**, and weather data loaded subsequently must **match the boundaries of an existing ACA**. (25/05/2026)

## 7. Infrastructure

Teams may host **remote services and the remote database on external cloud infrastructure of their choice**, provided the project requirements are met. **Any costs incurred are the team's responsibility**. (22/05/2026)

## 8. Reports

The client expects a **seamless experience**; how the **Java system and the C simulation module integrate** is an implementation decision left to the team rather than a client-facing requirement. (26/05/2026)

The simulation report is **stored as a file in the file system**, as stated in the user story. It is **not considered transient**. (26/05/2026)

"Monthly" means the report relates to a **calendar month**, as with monthly invoicing reports. (03/06/2026)

US112 should be built around a strategy pattern so it can serve as the foundation for future report types sharing a consistent structure. Only one example report needs to be provided, and the team may choose where the report's data is sourced from. (03/06/2026)

The granularity of the violation summary — listing individual violations versus aggregate counts — is left to the team. (03/06/2026)

A plain text file is sufficient for the report output. Richer formats or graphs are optional and positively viewed, not required. (03/06/2026)

A Flight Control Operator may only generate reports for the air control area(s) they are responsible for, not for areas they do not work in. (03/06/2026)

The simulator must receive the data it needs to run the simulation. Whether those parameters are read from the system's domain objects or supplied to the simulator by some other means is an implementation decision; the only requirement is that the necessary data reaches the simulator. (05/06/2026)

## 9. Simulation

The flight simulation in US100 and the related SCOMP user stories does not use a physical model — it does not actually simulate flight physics. That component (process coordination, position tracking, safety-conflict detection) is implemented in C. (03/06/2026)

The **simulation in US085 is a separate and different matter**: the physics-based test of the flight plan (fuel sufficiency, minimum-altitude compliance). It **may be implemented in either C or Java**. (03/06/2026)

## 10. Backlog and Scope

User stories that are in the product backlog but not in the current sprint backlog are not out of scope. They remain available for inclusion in future sprints. (05/06/2026)
