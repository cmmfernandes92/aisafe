# Invariants

## AirControlArea Aggregate
### Invariant ACA1
The AreaCode of an AirControlArea must be unique within the system.

### Invariant ACA2
The GeographicBoundary of an AirControlArea must define a valid closed polygon with at least three vertices.

### Invariant ACA3
All coordinates that compose the GeographicBoundary must be geographically valid.

## Aircraft Aggregate

### Invariant AC1
The Registration of an Aircraft must be unique within the system.

### Invariant AC2
An Aircraft must always be associated with exactly one AirTransportCompany.

### Invariant AC3
An Aircraft must always be associated with exactly one AircraftModel.

### Invariant AC4
The EngineModel used by an Aircraft must be certified for the associated AircraftModel.

### Invariant AC5
The total number of seats defined in the CabinConfiguration must not exceed the maximum capacity allowed by the AircraftModel.

### Invariant AC6
An Aircraft with OperationalStatus `RETIRED` must not be used in the creation of new Flights.

## AircraftModel Aggregate
### Invariant A1
The combination of aircraft model name and manufacturer must be unique.

### Invariant A2
An AircraftModel must retain at least one certified EngineModel.

### Invariant A3
The EngineModel associated to the AircraftModel must have a compatible motorization type.

## AirTransportCompany Aggregate

### Invariant ATC1
The CompanyIATACode of an AirTransportCompany must be unique within the system and consist of exactly two letters.

### Invariant ATC2
The CompanyICAOCode of an AirTransportCompany must be unique within the system and consist of two to three letters.

## CompanyCollaborator Aggregate

### Invariant CC1
A CompanyCollaborator must always be associated with exactly one AirTransportCompany.

### Invariant CC2
A CompanyCollaborator must always reference exactly one User.

### Invariant CC3
A CompanyCollaborator with status `DISABLED` must not be considered in active collaborator listings.

## EngineModel Aggregate
### Invariant E1
The combination of engine model name and manufacturer must be unique.

## Flight Aggregate
### Invariant F1
The FlightDesignator must be unique within the system and follow the format xxn(n)(n)(n)(a).

### Invariant F2
The flight type is determined by the Departure subclass: CharterDeparture defines a single departure date and time; RegularDeparture defines one or more WeekdaySchedules, each with one or more DepartureTimes.

### Invariant F3
The Aircraft assigned to a Flight must not have a retired OperationalStatus.

### Invariant F4
The Pilot assigned to a Flight must belong to the same AirTransportCompany as the FlightRoute.

### Invariant F5
A Flight must reference exactly one FlightRoute, one Aircraft, and one Pilot.

### Invariant F6
A Flight belongs to the AirTransportCompany that owns its FlightRoute.

### Invariant F7
A Flight must have at most one FlightPlan with status VALID.

### Invariant F8
In a sequence of FlightLegs, the arrival airport of leg n must equal the departure airport of leg n+1.

### Invariant F9
In a sequence of FlightLegs, the arrival time of leg n must precede the departure time of leg n+1.

### Invariant F10
The FuelLoad of each FlightLeg must be strictly positive.

### Invariant F11
Each FlightLeg must contain at least one FlightSegment.

### Invariant F12
The start and end Waypoints of a FlightSegment must be different.

### Invariant F13
AltitudeSlot values must be positive.

### Invariant F14
WindDirection must be between 0 and 360 degrees.

### Invariant F15
WindSpeed must be greater than or equal to zero.

## FlightRoute Aggregate
### Invariant FR1
The RouteName must be unique within the system and follow the format of two letters followed by up to four numbers.

### Invariant FR2
A FlightRoute must connect exactly two Airports, and the origin and destination must be different.

## Maker Aggregate
### Invariant M1
A Maker must always be identified by a valid Name and based in a valid Country.

## Pilot Aggregate

### Invariant P1
A Pilot must always be associated with exactly one AirTransportCompany.

### Invariant P2
A Pilot must always reference exactly one User.

### Invariant P3
A Pilot must have at least one PilotCertification associated with an AircraftModel.

### Invariant P4
A Pilot with assigned Flights must not be set to an inactive state.

## SimulationRun Aggregate
### Invariant SR1
A SimulationRun must reference at least one Flight.

### Invariant SR2
The TimeRange start must precede the TimeRange end.

### Invariant SR3
The safetyThreshold must be a positive value.

### Invariant SR4
SimulationStatus transitions must follow the sequence: CREATED → RUNNING → COMPLETED or FAILED.

### Invariant SR5
A SimulationResult may only be present when the SimulationStatus is COMPLETED.

### Invariant SR6
The ViolationPosition of each SafetyViolation must contain valid coordinates.

## User Aggregate
### Invariant U1
The Email of a User must be unique within the system.

### Invariant U2
The Email domain of a User must belong to the configured list of allowed email domains.

### Invariant U3
A User must have exactly one SystemRole.

### Invariant U4
A User must have a valid UserStatus (`ACTIVE` or `DISABLED`).

### Invariant U5
A User must have one active SecurityClearance with a valid expiration date.

### Invariant U6
A User must have one SkillsAssessment whose validity period does not exceed 5 years.

### Invariant U7
UserStatus transitions must be controlled and limited to valid state changes (`ACTIVE <-> DISABLED`).

## WeatherData Aggregate
### Invariant WD1
Each WeatherData record must be associated with exactly one AirControlArea.

### Invariant WD2
The recordDate must be a valid date.

### Invariant WD3
The temperature, atmosphericPressure, and visibility values must fall within valid physical ranges.

### Invariant WD4
The WeatherWindDirection must be between 0 and 360 degrees.

### Invariant WD5
The WeatherWindSpeed must be greater than or equal to zero.

### Invariant WD6
Duplicate records for the same AirControlArea, recordDate, and dataSource are not allowed.

## Notes
These invariants were selected because they are explicit in the requirements or directly implied by the domain rules used in the model.