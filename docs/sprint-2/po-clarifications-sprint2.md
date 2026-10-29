# PO Clarifications - Sprint 2

## 1. Users

For AISafe internal users, a list of **valid email domains should be bootstrapped** into the system. For external collaborators such as ATCC and pilots, **domain verification is not feasible and should not be enforced**. (20/04/2026)

**Security clearance requirements apply to all system users.** Administrators and Backoffice Operators may update security clearance information. **Skills assessment is a separate user attribute, not part of security clearance.** (22/04/2026)

Disabling a user **does not cascade** into removing their collaborator associations; it is **strictly a status change**. (28/04/2026)

An **expired security clearance prevents the user from logging into the system** but **does not deactivate the account**. An **outdated skills assessment has no effect** on the ability to log in or perform work. (30/04/2026)

## 2. Air Transport Companies

The **"position" field on a collaborator refers to their role within the company**. (22/04/2026)

**Company name, IATA code, and ICAO code must each be individually unique** across the system; **no two companies may share the same value** for any of these three identifiers. (22/04/2026, 27/04/2026)

## 3. Air Control Areas

The **area code has no prescribed format beyond system-wide uniqueness**. The **area name must also be unique**, as it is the identifier used in practice within the air control business. (23/04/2026)

Coordinate boundaries must be **valid per standard geographic ranges**; the boundary must **define a non-zero area**; and **no two areas may overlap**. A **Value Object for coordinates** naturally enforces range validity at construction time. (23/04/2026)

An **air control area does not require airports at creation time**. **Airports are registered separately** and **must fall within an existing area's boundaries**. (08/05/2026)

For the purposes of the MVP, an **Air Control Area is treated as the customer entity**. A **Flight Control Operator collaborator works in one Air Control Area at a time**. While a company may own several areas in practice, **they operate independently and this need not be modelled**. (02/05/2026, 12/05/2026)

## 4. Airports

If an airport's coordinates **do not fall within the boundaries** of the intended air control area, the creation **must be rejected as a business rule violation**. (23/04/2026)

**Country data should be bootstrapped.** The bootstrapped country list is shared reference data for `Airport`, `Aircraft`, and `Maker`. **Town name may be entered as free text** when creating an airport. (04/05/2026)

## 5. Weather Data

The **time period (start and end time) of a weather data record is defined by the user at registration time**. **No specific granularity is prescribed.** (28/04/2026)

**Overlapping weather records for the same area and time period reflect real-world complexity.** For the MVP, a **simplified approach is acceptable** provided the simplifications and their rationale are **documented**. (02/05/2026)

## 6. Aircraft and Engine Models

Aircraft model names have **no prescribed format**; examples include A320, A330-neo, B737 Max 8. The **combination of model name and manufacturer must be unique**. A **model identifier cannot be null**. (29/04/2026)

**Makers must be managed as entities, not Value Objects**, since multiple models share the same maker. **Maker names must be unique in a case-insensitive manner.** Makers are **not a fixed list**; new ones can be added, but a **predefined set may be bootstrapped** to ease testing. (29/04/2026)

**Fuel types should be pre-loaded via bootstrap**, but new types may need to be added in the future. An **engine model works with a limited number of compatible fuel types**. There is **no requirement to differentiate aircraft manufacturers from engine manufacturers**; a single maker may produce both. (01/05/2026, 08/05/2026)

A **propeller engine is characterised by its rated power**; **thrust is a product of the engine and propeller combination**. Different propeller and motor combinations produce different thrust curves. (11/05/2026)

## 7. Flights and Flight Plans

A flight plan progresses through the following states: **draft** (on creation via US080), **validated** (DSL validation as part of US085), and **tested** (simulation test as part of US085). **Adding weather data via US082 does not introduce a new state.** When **importing a flight plan from a file (US081), DSL validation must be performed**. (01/05/2026)

The **flight plan identifier is an internal system concern**. From a business perspective, a **flight is uniquely identified by its route and the date on which it operates**. A given **route cannot have more than one flight on the same day, considering local time**. (04/05/2026)

An **aircraft can only be decommissioned if it has no pending flights**. **Only flights with validated flight plans are considered pending**; draft plans do not count. (13/05/2026)

## 8. Infrastructure

**H2 in server mode running in a Docker container is an acceptable RDBMS deployment** for the final sprint review. **PostgreSQL with the PostGIS extension** is noted as a **beneficial alternative for simplifying coordinate-based queries**. (13/05/2026)
