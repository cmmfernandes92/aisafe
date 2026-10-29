# Glossary — Sprint 2

This glossary defines the terminology, value objects, entities, and acronyms used throughout the AISafe domain. Cross-cutting acronyms and actor definitions are listed first, followed by a dedicated table for each aggregate so that terms which appear in more than one context are read against the right aggregate.

---

## Acronyms

| TEA (Terms, Expressions, and Acronyms) | Description |
|:--------------------------------------|:------------|
| IATA | International Air Transport Association. Industry body that assigns standardized codes to airlines and airports. |
| ICAO | International Civil Aviation Organization. United Nations agency that assigns standardized codes to airlines and airports. |
| ITU | International Telecommunication Union. UN agency that maintains, among other things, the country-code allocations used in this system as the canonical `CountryCode`. |
| DSL | Domain Specific Language. Textual language used to formally describe flight plans. Processed using ANTLR for lexical, syntactic, and semantic validation before import into the system. |
| MTOW | Maximum Take-Off Weight. The maximum weight at which an aircraft model is certified for take-off. |
| MZFW | Maximum Zero Fuel Weight. The maximum weight of an aircraft model excluding usable fuel. Must be less than MTOW. |
| AR | Aspect Ratio. Ratio of wingspan squared to wing area, used in aerodynamic drag calculations. Not stored as an attribute — derived at simulation time from wing geometry. |
| TSFC | Thrust Specific Fuel Consumption. Measure of engine fuel efficiency expressed as fuel consumption per unit of thrust. Used in range and fuel calculations during simulation. |
| RBAC | Role-Based Access Control. Authorization model used by the backoffice: each user is assigned one `AisafeRoles` value that determines the use cases they may invoke. |
| DDD | Domain-Driven Design. Architectural style adopted across the project: model organised in aggregates with explicit roots and value objects. |

---

## Actors / System-wide concepts

| TEA | Description |
|:----|:------------|
| Administrator | System actor responsible for managing users, security clearances, and skills assessments. Has full control over backoffice user lifecycle. |
| Backoffice Operator | System actor responsible for registering and managing base system data such as airports, air control areas, aircraft models, engine models, manufacturers, air transport companies, and their collaborators. |
| ATCC | Air Transport Company Collaborator. System actor working on behalf of an air transport company; managed as a `CompanyCollaborator` in the `Collaborator` aggregate. |
| FCO | Flight Control Operator. System actor responsible for testing flight plans and running air control simulations within an air control area. Managed as an `AreaCollaborator` in the `Collaborator` aggregate. |
| Pilot | System actor authorised to operate aircraft for an air transport company and to import / test flight plans via the DSL. Modelled by the `Pilot` aggregate. |
| Weather Person | System actor responsible for registering, importing, and consulting weather data for air control areas. Accesses the system remotely via a dedicated TCP-based client application. |
| Authentication | Process by which a user proves their identity (username + password) before being granted access to the backoffice. |
| Authorization | Process by which the system, after authentication, decides whether the user's role (`AisafeRoles`) permits the requested action. |

---

## Maker Aggregate

| TEA | Description |
|:----|:------------|
| Maker | Manufacturer of aircraft models or engine models. A single manufacturer may produce both. |
| Maker Name | Human-readable name of a manufacturer. Must be unique system-wide in a case-insensitive manner. |

---

## AircraftModel Aggregate

| TEA | Description |
|:----|:------------|
| Aircraft Model | Motorized commercial aircraft model with associated technical characteristics and certified engines. |
| Model ID | Unique, non-null identifier assigned to an aircraft model within the system. |
| Model Name | Name that identifies an aircraft model, unique in combination with its `Maker`. |
| Empty Weight | Weight of the aircraft model without fuel, payload, or passengers. |
| Maximum Fuel Capacity | Maximum quantity of fuel that an aircraft model can carry. |
| Service Ceiling | Maximum altitude at which an aircraft model can sustain level flight under standard conditions. |
| Cruise Speed | Standard cruising speed of an aircraft model. |
| Wing Area | Surface area of the wings of an aircraft model, used in aerodynamic calculations. |
| Drag Coefficient | Aerodynamic drag coefficient of an aircraft model, used in performance calculations. |
| Lift Coefficient | Aerodynamic lift coefficient of an aircraft model, used in performance calculations. |
| Maximum Range | Maximum operational range of an aircraft model. |
| Aircraft Type | Operational type of an aircraft model: `PASSENGER`, `CARGO`, or `MIXED`. |
| Motorization Type | Required engine motorization for the aircraft model. Every certified engine model attached to the aircraft model must share this motorization. See the `EngineModel` table for the individual values. |

---

## EngineModel Aggregate

| TEA | Description |
|:----|:------------|
| Engine Model | Engine model with technical characteristics, referenced by aircraft models for certification. |
| Engine Model Name | Name that identifies an engine model, unique in combination with its `Maker`. |
| Power | Rated power output of an engine model. For propeller engines, the engine is characterised by rated power; thrust is a product of the engine + propeller combination. |
| Efficiency | Fuel or energy efficiency rating of an engine model. |
| Motorization Type | Motorization classification of the engine. Used to verify compatibility with an aircraft model's motorization. Values: `Turboprop`, `Turbofan`, `Turbojet`, `Ramjet`, `Electric Propeller`. |
| Turboprop | Gas-turbine engine that drives a propeller through a reduction gearbox. Typical of short-haul regional aircraft; efficient at lower airspeeds. |
| Turbofan | Gas-turbine engine with a large fan at the front that accelerates bypass air around the core, producing most of the thrust. Standard for modern commercial jets. |
| Turbojet | Pure gas-turbine engine where all intake air passes through the core and is expelled as high-velocity exhaust. Higher specific fuel consumption than turbofans; used in older airliners and some military aircraft. |
| Ramjet | Air-breathing engine with no rotating compressor; relies on the vehicle's forward motion to compress incoming air. Operates only above significant airspeed; typical of high-speed/research aircraft. |
| Electric Propeller | Propeller driven by an electric motor powered by an on-board energy store (e.g. battery). Used by electric and hybrid-electric aircraft. |
| Fuel Type | Enumerated set of fuel types an engine model can consume. Bootstrapped at startup; new entries can be added in future iterations. Values: `Jet A`, `Jet A1`, `Avgas`, `Hydrogen`, `Electric`, `Sustainable Aviation Fuel`. |
| Jet A | Kerosene-based commercial jet fuel widely used in North America (flash point ≥ 38°C, freeze point ≤ −40°C). |
| Jet A1 | International variant of Jet A with a lower freeze point (≤ −47°C), used outside North America. |
| Avgas | Aviation gasoline used by piston-engine aircraft (typically AVGAS 100LL). |
| Hydrogen | Liquid or gaseous hydrogen, modelled as a future-aviation fuel option for hydrogen-powered engines. |
| Electric | "Fuel" entry representing electric energy storage (battery) for `Electric Propeller` engines. |
| Sustainable Aviation Fuel | Drop-in or blended sustainable aviation fuel (SAF) derived from non-fossil feedstocks. |

---

## User Aggregate

| TEA | Description |
|:----|:------------|
| User | Person with access to the AISafe system, identified by a unique email address. |
| Full Name | Complete personal name of a system user. |
| Email | Email address used to uniquely identify a system user. For internal AISafe roles, the email must belong to the bootstrapped list of approved domains; external collaborators (ATCC, FCO, Pilot) are not subject to domain verification. |
| Phone Number | Telephone contact number of a user. Stored as a value object; basic international format validation is enforced at construction. |
| Credential | Value object that encapsulates the user's stored password as a hash, plus the password-validation logic. Plain-text passwords are never persisted. |
| Security Clearance | Security clearance held by an AISafe user, with an expiration date. |
| Expiration Date | Date on which a user's security clearance expires. An expired clearance prevents login but does not change `UserStatus`. |
| Skills Assessment | Periodic assessment record that evaluates a user's skills and regulatory fitness. Independent of `SecurityClearance`. |
| Assessment Score | Numeric score resulting from a user's skills assessment. |
| Assessment Period | Time interval during which a skills assessment is considered valid. Must not exceed 5 years per regulation. |
| AisafeRoles | Enumerated role of a user, controlling which use cases are available: `Admin`, `Backoffice Operator`, `Air Transport Company Collaborator`, `Pilot`, `Flight Control Operator`, or `Weather Person`. Replaces sprint-1's `SystemRole`. |
| User Status | Status indicating whether a user is `Active` or `Disabled`. Transitions are performed by an Administrator and do not cascade to collaborator or pilot associations. |

---

## Collaborator Aggregate

| TEA | Description |
|:----|:------------|
| Collaborator | Abstract aggregate root representing any non-administrative user who works on behalf of a customer-side entity. Specialised as `CompanyCollaborator` or `AreaCollaborator`. Each collaborator references exactly one `User`; one `User` can be the identity of at most one collaborator at any given time. |
| Company Collaborator | Collaborator associated with an air transport company who uses the system on behalf of that company. Identity `User` must have `AisafeRoles = Air Transport Company Collaborator`. |
| Area Collaborator | Collaborator associated with an air control area (Flight Control Operator). Identity `User` must have `AisafeRoles = Flight Control Operator`. A given FCO works in exactly one air control area at a time. |
| Position | Role or job position held by a collaborator within their customer entity (air transport company or air control area). Must not be blank; immutable post-creation. |
| Collaborator Status | Status indicating whether a collaborator is `Active` or `Disabled`. Defaults to `Active` on creation. Disabled collaborators are excluded from listings. |

---

## Pilot Aggregate

| TEA | Description |
|:----|:------------|
| Pilot | Person certified to operate aircraft on behalf of an air transport company. Modelled as its own aggregate, distinct from the `Collaborator` aggregate. |
| Pilot Certification | Certification that authorizes a pilot to operate a specific aircraft model. |
| Pilot Status | Status indicating whether a pilot is `Active` or `Inactive`. A pilot with assigned flight plans cannot be made inactive. |

---

## Aircraft Aggregate

| TEA | Description |
|:----|:------------|
| Aircraft | Aircraft of a specific model and engine configuration operated by a company. |
| Crew Count | Number of flight crew members assigned to a specific aircraft. |
| Registration | Unique aircraft registration number, used as the natural identifier of a specific aircraft. |
| Cabin Configuration | Distribution of seats by travel class inside the aircraft cabin. |
| Economy Seats | Number of economy class seats in a cabin configuration. |
| Business Seats | Number of business class seats in a cabin configuration. |
| First Class Seats | Number of first class seats in a cabin configuration. |
| Seat Class | Enumerated travel-class identifier (`Economy`, `Business`, `First`) used by `CabinConfiguration` to describe per-class seat counts. |
| Operational Status | Status indicating whether an aircraft is `Active` or `Retired`. Retired aircraft cannot be used in new flights. An aircraft can only transition `Active → Retired` when it has no pending flights (i.e. no flights with a `Validated` flight plan). |
| Registration Country | Country in which a specific aircraft is registered. Referenced as the `Country` aggregate root (not as a value object). |

---

## AirControlArea Aggregate

| TEA | Description |
|:----|:------------|
| Air Control Area | Controlled airspace area managed for flight monitoring and simulation purposes. In the MVP, plays the role of the customer entity for Flight Control Operators. |
| Area Code | Unique business code assigned to an air control area. No prescribed format beyond system-wide uniqueness. |
| Area Name | Human-readable identifier of an air control area, also unique system-wide as it is the identifier used in practice in the air-control business. |
| Geographic Boundary | Spatial boundary that defines the extent of an air control area. Must define a non-zero rectangle (SW strictly less than NE on both axes) and must not overlap with any other area's boundary. |
| Coordinates | Latitude / longitude pair (degrees) that delimits the corners of a `GeographicBoundary`. Range enforced at construction: latitude in [-90, 90], longitude in [-180, 180]. |

---

## Airport Aggregate

| TEA | Description |
|:----|:------------|
| Airport | Airport used as origin, destination, or stop in flight operations. |
| Airport IATA Code | Three-letter IATA identifier of an airport. Unique worldwide. |
| Airport ICAO Code | Four-letter ICAO identifier of an airport. Unique worldwide. |
| Airport Name | Official name of an airport. Must not be blank. |
| Town | Town or city in which an airport is located. Free-text input (no prescribed taxonomy). |
| Elevation | Height of an airport above mean sea level, expressed in metres. Negative values are allowed (airports below sea level). |
| Airport Coordinates | Value object holding the geographic coordinates (latitude and longitude) of an airport. Must fall within the boundaries of the associated `AirControlArea`. |

---

## Country Aggregate

| TEA | Description |
|:----|:------------|
| Country | Nation in which an entity is registered or located. Referenced by airports (location), aircraft (registration country), and makers (country of origin). Country data is bootstrapped at system startup. |
| Country Code | Stable ITU-assigned country identifier used as the immutable key for the `Country` aggregate. The display `name` may change over time; `CountryCode` does not. |

---

## AirTransportCompany Aggregate

| TEA | Description |
|:----|:------------|
| Air Transport Company | Airline or air transport company registered in the system. |
| Company Name | Official registered name of an air transport company. Must be unique system-wide (in addition to IATA and ICAO codes). |
| Company IATA Code | Two-letter IATA code that identifies an air transport company. Unique system-wide. |
| Company ICAO Code | Two- or three-letter ICAO code that identifies an air transport company. Unique system-wide. |

---

## FlightRoute Aggregate

| TEA | Description |
|:----|:------------|
| Flight Route | Named connection between an origin and a destination airport, operated by an air transport company. |
| Route Name | Designation used to identify a flight route (format: 2 letters + up to 4 numbers). Unique system-wide. |
| Route Validity | Status indicating whether a route is `Active` and available for use, or `Inactive`. |

---

## Flight Aggregate

| TEA | Description |
|:----|:------------|
| Flight | Concrete scheduled operation on a specific route, with a unique designator, aircraft, pilot, and departure. An instantiation of a route. |
| Flight Designator | Unique identifier of a flight, composed of the airline designator and flight number (format: xxn(n)(n)(n)(a)). |
| Departure | Abstract value object representing the departure specification of a flight. Specialized as `RegularDeparture` or `CharterDeparture`. |
| Charter Departure | Departure specification for a charter flight, defined by a single specific date and time. |
| Charter Flight | A one-off flight operation scheduled for a specific date and time, as opposed to a recurring regular flight. |
| Regular Departure | Departure specification for a regular flight, defined by one or more weekday schedules, each with one or more departure times. |
| Regular Flight | A recurring flight operation that follows a repeating schedule of weekdays and times, as opposed to a one-off charter flight. |
| Weekday Schedule | Association of a specific weekday with one or more departure times, used in regular flight scheduling. |
| Weekday | Day of the week associated with a weekday schedule in a regular flight departure. |
| Departure Date | The specific date on which a charter flight departs. |
| Departure Time | Specific time of day at which a flight departs, associated with a weekday schedule for regular flights. |
| Flight Plan | Complete operational plan describing how a flight is executed, including legs, segments, waypoints, and fuel. Progresses through `Draft → Validated → Tested`. At most one plan per flight may be `Validated` at any time. |
| Flight Plan ID | Internal-system identifier of a flight plan within its flight; not part of the public business identity. |
| Flight Plan Status | Status of a flight plan. Values: `Draft` (on creation via US080), `Validated` (after DSL validation in US085 or file import in US081), or `Tested` (after simulation test in US085). |
| Draft | Initial flight-plan status assigned on creation. A draft plan is editable and is not considered when checking pending flights for aircraft decommissioning. |
| Validated | Flight-plan status reached after successful DSL lexical/syntactic/semantic validation. At most one validated plan may exist per flight; validated plans are the ones that count as "pending" for aircraft decommissioning. |
| Tested | Flight-plan status reached after a successful simulation test of a validated plan. |
| Flight Leg | Non-stop journey segment between two airports within a flight plan. |
| Departure Info | Departure details of a flight leg, including airport and time-related information. |
| Arrival Info | Arrival details of a flight leg, including airport and time-related information. |
| Arrival Time | Planned local arrival time of a flight, derived from the valid flight plan's last leg arrival info. |
| Fuel Load | Quantity of fuel assigned to a given flight leg. Must be strictly positive. |
| Flight Segment | Linear route segment between two waypoints within a flight leg. |
| Waypoint | Geographic navigation point used to define the start or end of a flight segment. |
| Coordinates | Latitude / longitude pair identifying a waypoint's geographic point. |
| Altitude Slot | Allowed altitude range or slot available for flight in a segment. Values must be positive. |
| Wind Direction | Wind direction angle relative to North, in degrees [0, 360]. Used within flight segments. |
| Wind Speed | Speed of the wind in m/s, ≥ 0. Used within flight segments. |

---

## WeatherData Aggregate

| TEA | Description |
|:----|:------------|
| Weather Data | Weather information registered for a specific air control area over a given time period. |
| Weather Data ID | Value object that uniquely identifies a weather data record. |
| Temperature | Atmospheric temperature measured for a weather record. |
| Atmospheric Pressure | Pressure of the atmosphere recorded for weather analysis. |
| Visibility | Measured atmospheric visibility for a weather record. |
| Time Period | Value object capturing the `startTime` and `endTime` of a weather record. Granularity is defined by the user at registration time (no prescribed step). `startTime` must precede `endTime`. Replaces sprint-1's single `recordDate`. |
| Weather Wind Direction | Value object representing wind direction within a weather data record, in degrees [0, 360] relative to North. |
| Weather Wind Speed | Value object representing wind speed within a weather data record, in m/s, ≥ 0. |
| Data Source | Value object identifying the origin or provider of a weather data record. |

---

## SimulationRun Aggregate

| TEA | Description |
|:----|:------------|
| Simulation Run | Execution of a flight simulation in a given air control area, with configured parameters and referenced flights. |
| Simulation ID | Unique identifier assigned to a simulation run. |
| Safety Threshold | Minimum safety distance or threshold enforced during simulation. Must be positive. Crossing it may trigger early termination of the run. |
| Time Range | Start and end time interval used to scope a simulation run. Start must precede end. |
| Performance Settings | Configuration parameters that affect simulation execution behaviour. |
| Simulation Status | Lifecycle status of a simulation run: `Created → Running → Completed \| Failed`. |
| Simulation Result | Output produced by a completed simulation run, including validation outcome and safety violations. Present only when status is `Completed`. |
| Validation Outcome | Result of simulation validation: `Pass` (zero safety violations) or `Fail`. |
| Safety Violation | Detected air safety violation during simulation, including timestamp and position. |
| Violation Timestamp | Date and time at which a safety violation occurred during a simulation run. |
| Violation Position | Geographic position at which a safety violation was detected during simulation. |