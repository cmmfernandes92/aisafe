# RCOMP: Remote Access and Logging Protocols

## 1. Scope

This planning document covers the **networking view** of the Sprint 3 remote-access user stories:

* US044 — Weather Person remote access;
* US078 — Air Transport Company Collaborator remote access;
* US086 — Pilot remote access;
* US090 — External logging of remote accesses;
* US091 — Remote accesses logging visualization.

Application and domain design are documented in the respective user-story folders under `docs/sprint-3/`.

This document focuses on:

* socket communication;
* wire protocols;
* client-server separation;
* concurrency;
* authentication and authorization;
* remote-access logging;
* deployment.

The deployment target follows the Project 2 deployment model: server applications run on separate remote/cloud nodes, while client applications may run on the students' computers.

---

## 2. Global decisions

* **One TCP server per remote actor**, embedded in the corresponding AISafe console application.
* **Thread-per-connection concurrency model.** Each TCP server continuously accepts new connections and starts one handler thread per connected client.
* **Authentication and authorization at application level.** Credentials are transmitted through the application protocol, and each service validates the required AISafe role.
* **Thin TCP clients.** Remote clients use `java.net` and do not directly access AISafe repositories, persistence, JPA, JDBC, or the database.
* **Separate wire protocols are allowed.** Each remote-access service is self-contained and may use a protocol appropriate to its requirements.
* **Remote-access logging is transmitted through UDP** to a dedicated Logging Server, independently from the TCP remote-access services and the AISafe database.
* **No TLS channel encryption** is included in the current sprint scope.

---

## 3. US044 — Weather Person remote access

TCP service embedded in the Weather console application.

| Item               | Decision                    |
| ------------------ | --------------------------- |
| Role               | `WEATHER_PERSON`            |
| TCP port           | `5556`                      |
| Server             | `WeatherRemoteServer`       |
| Connection handler | `WeatherClientHandler`      |
| Client             | `WeatherPersonRemoteClient` |
| Protocol           | Text-based, line-oriented   |
| Character encoding | UTF-8                       |

### 3.1. Framing

TCP provides a byte stream and does not preserve message boundaries.

The Weather service uses a line-oriented protocol:

* commands and simple responses are transmitted as individual lines;
* multi-row responses start with `OK`, contain zero or more data lines, and finish with `END`;
* CSV imports use a declared character count and a two-phase handshake.

### 3.2. Concurrency

`WeatherRemoteServer` uses a `ServerSocket` accept loop.

For each accepted connection, the server starts a dedicated `WeatherClientHandler` thread.

### 3.3. Authentication-session protection

The AISafe authorization framework maintains shared session state inside the Weather application process.

To prevent concurrent TCP clients from overwriting each other's authentication session, `AuthGate` serializes:

```text
authenticate → execute use case → clear session
```

inside one JVM-wide `synchronized` block.

### 3.4. Protocol commands

```text
PING
LOGIN
LOGOUT
EXIT
AREAS
WCONSULT
WREG
WIMPORT
```

Response prefixes:

```text
OK
ERR
END
```

### 3.5. Weather operations

* `AREAS` — lists the registered Air Control Areas;
* `WCONSULT` — remotely exposes weather-data consultation;
* `WREG` — remotely exposes weather-data registration;
* `WIMPORT` — remotely exposes bulk weather-data import from CSV.

`WREG` sends the required weather fields as a pipe-separated request.

`WIMPORT` uses the following two-phase exchange:

```text
Client → WIMPORT <areaCode>|<dataSource>|<charCount>
Server → OK READY
Client → <exactly charCount characters of CSV content>
Server → OK <summary>
         [row-error lines]
         END
```

Invalid metadata is rejected before the CSV body is transmitted.

### 3.6. Logging integration

The handler emits US090 remote-access events for:

* successful login;
* failed login;
* logout;
* unexpected disconnection.

The service identifier is `US44`.

---

## 4. US086 — Pilot remote access

TCP service embedded in the Pilot console application.

| Item               | Decision                      |
| ------------------ | ----------------------------- |
| Role               | `PILOT`                       |
| TCP port           | `5555`                        |
| Server             | `PilotTcpServer`              |
| Connection handler | `PilotConnectionHandler`      |
| Client             | `PilotRemoteClient`           |
| Protocol           | Binary, opcode-based          |
| Framing            | Length-prefixed UTF-8 strings |

### 4.1. Framing

Strings are transmitted using:

```text
[2-byte length][UTF-8 bytes]
```

The receiver reads the declared number of bytes before processing the value.

Commands and responses use binary opcode values defined in `TcpProtocol`.

### 4.2. Concurrency

The Pilot server uses an accept loop and starts one `PilotConnectionHandler` thread per client connection.

### 4.3. Session

The first message must be `LOGIN`.

Invalid credentials or users without the `PILOT` role are rejected.

### 4.4. Protocol operations

The protocol includes operations such as:

```text
LOGIN
LOGOUT
CREATE_FLIGHT_PLAN
CREATE_FROM_DSL
INSERT_WEATHER
VALIDATE_FLIGHT_PLAN
LIST_FLIGHTS
LIST_WEATHER
```

Response types include:

```text
RESPONSE_OK
RESPONSE_ERROR
RESPONSE_DATA
```

### 4.5. Logging integration

The Pilot handler emits US090 events using the service identifier `US86`.

---

## 5. US078 — Air Transport Company Collaborator remote access

TCP service embedded in the ATCC console application.

| Item               | Decision                             |
| ------------------ | ------------------------------------ |
| Role               | `AIR_TRANSPORT_COMPANY_COLLABORATOR` |
| TCP port           | `6666`                               |
| Server             | `AtccTcpServer`                      |
| Connection handler | `AtccConnectionHandler`              |
| Client             | `AtccRemoteClient`                   |
| Protocol           | Binary, opcode-based                 |

### 5.1. Concurrency

The ATCC server follows the same thread-per-connection model:

```text
accept connection → create AtccConnectionHandler thread
```

### 5.2. Client-server separation

`AtccRemoteClient` communicates with the AISafe application only through TCP.

The client does not directly access:

* the database;
* persistence;
* repositories;
* AISafe application controllers.

### 5.3. Authentication and authorization

The server authenticates the remote user and verifies the role:

```text
AIR_TRANSPORT_COMPANY_COLLABORATOR
```

### 5.4. Protocol

The ATCC service uses binary command opcodes defined in `AtccProtocol`.

The available commands remotely expose the applicable Air Transport Company Collaborator user stories.

### 5.5. Logging integration

The ATCC handler emits US090 events using the service identifier `US78`.

---

## 6. US090 — External logging of remote accesses

Remote-access events are transmitted to a dedicated Logging Server through UDP datagrams.

UDP is used as a fire-and-forget transport:

* remote-access services do not wait for a logging response;
* logging failures do not interrupt or delay login and logout operations;
* the logging system remains independent from the AISafe database.

### 6.1. Shared emitter

`RemoteAccessLogger` is located in:

```text
aisafe.app.common.console.remote.logging
```

It is shared by the Weather, Pilot, and ATCC remote-access services.

### 6.2. Events

Supported event types:

```text
LOGIN_OK
LOGIN_FAIL
LOGOUT
DISCONNECT
```

Each event contains:

```text
eventType|isoTimestamp|username|clientIp|clientPort|service
```

The service value identifies the remote-access US:

```text
US44
US78
US86
```

### 6.3. Logging Server

The standalone module:

```text
aisafe.app.logserver
```

contains the UDP receiver.

| Component            |   Port |
| -------------------- | -----: |
| UDP logging receiver | `9090` |

Received events are stored in an in-memory `LogStore`.

The store maintains:

* the most recent remote-access events;
* the set of currently connected users.

Its access methods are synchronized because UDP receiver and HTTP server threads share the same state.

---

## 7. US091 — Remote accesses logging visualization

The Logging Server also embeds an HTTP server that exposes the current contents of `LogStore`.

| Component   |   Port |
| ----------- | -----: |
| HTTP server | `8090` |

### 7.1. HTTP routes

```text
GET /
GET /events
GET /active
```

* `/` serves the main visualization page and static resources;
* `/events` returns the latest remote-access events;
* `/active` returns the users currently connected to the remote services.

### 7.2. AJAX updates

The browser uses AJAX to update the events and active-users areas without reloading the full page.

Each request schedules the next request only after completing or failing, avoiding the accumulation of concurrent requests when the server is slow or unavailable.

---

## 8. Deployment

| Node       | Application         | Components                                          |
| ---------- | ------------------- | --------------------------------------------------- |
| Cloud      | Logging Server      | UDP receiver on `9090` and HTTP server on `8090`    |
| Cloud      | Weather Application | `WeatherRemoteServer` on `5556` and UDP log emitter |
| Cloud      | Pilot Application   | `PilotTcpServer` on `5555` and UDP log emitter      |
| Cloud      | ATCC Application    | `AtccTcpServer` on `6666` and UDP log emitter       |
| Cloud      | Database            | AISafe SQL database                                 |
| Student PC | Weather client      | `WeatherPersonRemoteClient`                         |
| Student PC | Pilot client        | `PilotRemoteClient`                                 |
| Student PC | ATCC client         | `AtccRemoteClient`                                  |
| Student PC | Web browser         | HTTP client for the US091 visualization             |

---

## 9. Subtasks assignment

| Task                  | Member                      | Responsibility                                                |
| --------------------- | --------------------------- | ------------------------------------------------------------- |
| T.3.1 — US086         | Carolina Oliveira | Pilot remote access                                           |
| T.3.2 — US044         | Inês Barbosa      | Weather Person remote access and Sprint Master coordination   |
| T.3.3 — US090 & US091 | Carlos Fernandes  | External logging of remote accesses and logging visualization |
| T.3.4 — US078         | Rafael Monteiro   | Air Transport Company Collaborator remote access              |
