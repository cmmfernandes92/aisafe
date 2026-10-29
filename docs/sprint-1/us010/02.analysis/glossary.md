# Glossary

| TEA (Terms, Expressions, and Acronyms)  | Description |
|:----------------------------------------|:------------|
| Administrator (Admin)                   | System actor responsible for managing users, security clearances, and skills assessments. Has full control over backoffice user lifecycle. |
| Air Control Area                        | Controlled airspace area managed for flight monitoring and simulation purposes. |
| Air Transport Company                   | Airline or air transport company registered in the system. |
| Aircraft                                | Aircraft of a specific model and engine configuration operated by a company. |
| Aircraft Model                          | Motorized commercial aircraft model with associated technical characteristics and certified engines. |
| Aircraft Type                           | Operational type of an aircraft model: PASSENGER, CARGO, or MIXED. |
| Airport                                 | Airport used as origin, destination, or stop in flight operations. |
| Airport Coordinates                     | Value object holding the geographic coordinates (latitude and longitude) of an airport. |
| Airport IATA Code                       | Three-letter IATA identifier of an airport. |
| Airport ICAO Code                       | Four-letter ICAO identifier of an airport. |
| Airport Name                            | Official name of an airport. |
| Altitude Slot                           | Allowed altitude range or slot available for flight in a segment. |
| Area Code                               | Unique business code assigned to an air control area. |
| Arrival Info                            | Arrival details of a flight leg, including airport and time-related information. |
| Arrival Time                            | Planned local arrival time of a flight, derived from the valid flight plan's last leg arrival info. |
| Aspect Ratio (AR)                       | Ratio of wingspan squared to wing area, used in aerodynamic drag calculations. Not stored as an attribute — derived at simulation time from wing geometry. |
| Assessment Period                       | Time interval during which a skills assessment is considered valid. |
| Assessment Score                        | Numeric score resulting from a user's skills assessment. |
| Atmospheric Pressure                    | Pressure of the atmosphere recorded for weather analysis. |
| Backoffice Operator                     | System actor responsible for registering and managing base system data such as airports, air control areas, aircraft models, engine models, manufacturers, air transport companies, and their collaborators. |
| Business Seats                          | Number of business class seats in a cabin configuration. |
| Cabin Configuration                     | Distribution of seats by travel class inside the aircraft cabin. |
| Charter Departure                       | Departure specification for a charter flight, defined by a single specific date and time. |
| Charter Flight                          | A one-off flight operation scheduled for a specific date and time, as opposed to a recurring regular flight. |
| Collaborator Status                     | Status indicating whether a company collaborator is active or disabled. |
| Company Collaborator                    | Collaborator associated with an air transport company who uses the system on behalf of that company. |
| Company IATA Code                       | Two-letter IATA code that identifies an air transport company. |
| Company ICAO Code                       | Two- or three-letter ICAO code that identifies an air transport company. |
| Company Name                            | Official registered name of an air transport company. |
| Coordinates                             | Latitude and longitude pair identifying a geographic point. Used within waypoints. |
| Country                                 | Nation in which an entity is registered or located. Used to identify the origin of a manufacturer, the location of an airport, and the registration country of an aircraft. |
| Crew Count                              | Number of flight crew members assigned to a specific aircraft. |
| Cruise Speed                            | Standard cruising speed of an aircraft model. |
| DSL (Domain Specific Language)          | Textual language used to formally describe flight plans. Processed using ANTLR for lexical, syntactic, and semantic validation before import into the system. |
| Data Source                             | Origin or provider of a weather data record. |
| Departure                               | Abstract concept representing the departure specification of a flight. Specialized as Regular Departure or Charter Departure. |
| Departure Date                          | The specific date on which a charter flight departs. |
| Departure Info                          | Departure details of a flight leg, including airport and time-related information. |
| Departure Time                          | Specific time of day at which a flight departs, associated with a weekday schedule for regular flights. |
| Drag Coefficient                        | Aerodynamic drag coefficient of an aircraft model, used in performance calculations. |
| Economy Seats                           | Number of economy class seats in a cabin configuration. |
| Efficiency                              | Fuel or energy efficiency rating of an engine model. |
| Elevation                               | Height of an airport above mean sea level, expressed in metres. |
| Email                                   | Email address used to uniquely identify a system user. Must belong to the list of valid/allowed email domains. |
| Empty Weight                            | Weight of the aircraft model without fuel, payload, or passengers. |
| Engine Model                            | Engine model with technical characteristics, referenced by aircraft models for certification. |
| Engine Model Name                       | Name that identifies an engine model, unique in combination with its manufacturer. |
| Expiration Date                         | Date on which a security clearance expires. |
| First Class Seats                       | Number of first class seats in a cabin configuration. |
| Flight                                  | Concrete scheduled operation on a specific route, with a unique designator, aircraft, pilot, and departure. An instantiation of a route. |
| Flight Control Operator                 | System actor responsible for testing flight plans and running air control simulations within an air control area. Managed directly by the system administrator. |
| Flight Designator                       | Unique identifier of a flight, composed of the airline designator and flight number (format: xxn(n)(n)(n)(a)). |
| Flight Leg                              | Non-stop journey segment between two airports within a flight plan. |
| Flight Plan                             | Complete operational plan describing how a flight is executed, including legs, segments, waypoints, and fuel. A flight plan can be in draft or valid status; at most one plan per flight may be valid at any time. |
| Flight Plan ID                          | Unique identifier of a flight plan within its flight. |
| Flight Plan Status                      | Status of a flight plan: DRAFT or VALID. At most one flight plan per flight may have status VALID at any time. |
| Flight Route                            | Named connection between an origin and a destination airport, operated by an air transport company. |
| Flight Segment                          | Linear route segment between two waypoints within a flight leg. |
| Fuel Load                               | Quantity of fuel assigned to a given flight leg. |
| Fuel Type                               | Type of fuel used by an engine model. |
| Full Name                               | Complete personal name of a system user. |
| Geographic Boundary                     | Spatial boundary that defines the extent of an air control area. |
| IATA                                    | International Air Transport Association. Industry body that assigns standardized codes to airlines and airports. |
| ICAO                                    | International Civil Aviation Organization. United Nations agency that assigns standardized codes to airlines and airports. |
| Lift Coefficient                        | Aerodynamic lift coefficient of an aircraft model, used in performance calculations. |
| Maker                                   | Manufacturer of aircraft models or engine models. A single manufacturer may produce both. |
| Maximum Fuel Capacity                   | Maximum quantity of fuel that an aircraft model can carry. |
| Maximum Range                           | Maximum operational range of an aircraft model. |
| Model ID                                | Unique identifier assigned to an aircraft model within the system. |
| Model Name                              | Name that identifies an aircraft model, unique in combination with its manufacturer. |
| Motorization Type                       | Type of engine motorization: TURBOPROP, TURBOFAN, TURBOJET, RAMJET, or ELECTRIC_PROPELLER. |
| MTOW (Maximum Take-Off Weight)          | The maximum weight at which an aircraft model is certified for take-off. |
| MZFW (Maximum Zero Fuel Weight)         | The maximum weight of an aircraft model excluding usable fuel. Must be less than MTOW. |
| Operational Status                      | Status indicating whether an aircraft is ACTIVE or RETIRED. Retired aircraft cannot be used in new flights. |
| Performance Settings                    | Configuration parameters that affect simulation execution behaviour. |
| Phone Number                            | Telephone contact number of a user. |
| Pilot                                   | Person certified to operate aircraft on behalf of an air transport company. |
| Pilot Certification                     | Certification that authorizes a pilot to operate a specific aircraft model. |
| Pilot Status                            | Status indicating whether a pilot is ACTIVE or INACTIVE. |
| Position                                | Role or job position held by a company collaborator within the air transport company. |
| Power                                   | Power output of an engine model. |
| Record Date                             | Date on which a weather record applies or was collected. |
| Registration                            | Unique aircraft registration number, used as the natural identifier of a specific aircraft. |
| Registration Country                    | Country in which a specific aircraft is registered. |
| Regular Departure                       | Departure specification for a regular flight, defined by one or more weekday schedules, each with one or more departure times. |
| Regular Flight                          | A recurring flight operation that follows a repeating schedule of weekdays and times, as opposed to a one-off charter flight. |
| Route Name                              | Designation used to identify a flight route (format: 2 letters + up to 4 numbers). |
| Route Validity                          | Status indicating whether a route is ACTIVE and available for use, or INACTIVE. |
| Safety Threshold                        | Minimum safety distance or threshold enforced during simulation. |
| Safety Violation                        | Detected air safety violation during simulation, including timestamp and position. |
| Seat Count                              | Abstract concept representing the number of seats of a given travel class in a cabin configuration. Specialised as Economy Seats, Business Seats, and First Class Seats. |
| Security Clearance                      | Security clearance held by an AISafe user, with an expiration date. |
| Service Ceiling                         | Maximum altitude at which an aircraft model can sustain level flight under standard conditions. |
| Simulation ID                           | Unique identifier assigned to a simulation run. |
| Simulation Result                       | Output produced by a completed simulation run, including validation outcome and safety violations. |
| Simulation Run                          | Execution of a flight simulation in a given air control area, with configured parameters and referenced flights. |
| Simulation Status                       | Lifecycle status of a simulation run: CREATED, RUNNING, COMPLETED, or FAILED. |
| Skills Assessment                       | Periodic assessment record that evaluates a user's skills and regulatory fitness. |
| System Role                             | Role assigned to a user to define permissions and responsibilities in the system: ADMIN, BACKOFFICE_OPERATOR, AIR_TRANSPORT_COMPANY_COLLABORATOR, PILOT, FLIGHT_CONTROL_OPERATOR, or WEATHER_PERSON. |
| Temperature                             | Atmospheric temperature measured for a weather record. |
| TSFC (Thrust Specific Fuel Consumption) | Measure of engine fuel efficiency expressed as fuel consumption per unit of thrust. Used in range and fuel calculations during simulation. |
| Time Range                              | Start and end time interval used to scope a simulation run. |
| Town                                    | Town or city in which an airport is located. |
| User                                    | Person with access to the AISafe system, identified by a unique email address. |
| User Status                             | Status indicating whether a user is ACTIVE or DISABLED. |
| Validation Outcome                      | Result of simulation validation: PASS (zero safety violations) or FAIL. |
| Violation Position                      | Geographic position at which a safety violation was detected during simulation. |
| Violation Timestamp                     | Date and time at which a safety violation occurred during a simulation run. |
| Visibility                              | Measured atmospheric visibility for a weather record. |
| Waypoint                                | Geographic navigation point used to define the start or end of a flight segment. |
| Weather Data                            | Weather information registered for a specific air control area on a given date. |
| Weather Data ID                         | Unique identifier assigned to a weather data record. |
| Weather Person                          | System actor responsible for registering, importing, and consulting weather data for air control areas. Accesses the system remotely via a dedicated TCP-based client application. |
| Weather Wind Direction                  | Value object representing wind direction within a weather data record, in degrees (0–360) relative to North. |
| Weather Wind Speed                      | Value object representing wind speed within a weather data record, in m/s. |
| Weekday                                 | Day of the week associated with a weekday schedule in a regular flight departure. |
| Weekday Schedule                        | Association of a specific weekday with one or more departure times, used in regular flight scheduling. |
| Wind Direction                          | Wind direction angle relative to North, in degrees (0–360). Used within flight segments. |
| Wind Speed                              | Speed of the wind in m/s. Used within flight segments. |
| Wing Area                               | Surface area of the wings of an aircraft model, used in aerodynamic calculations. |