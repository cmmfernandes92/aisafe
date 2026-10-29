# Flight DSL — Semantic Specification (US120)

## 1. Introduction

This document specifies the **semantic rules** of the Flight DSL introduced in US083.
It is a companion to [`docs/sprint-2/us083/dsl-spec.md`](../../sprint-2/us083/dsl-spec.md),
which covers the lexical and syntactic specification; readers should consult that document
first.

The semantic layer is delivered as two Maven modules with distinct roles:

| Module | Role |
|---|---|
| `aisafe/aisafe.flightplandsl` | **The wired pipeline (EAPLI / Java integration).** ANTLR grammar, lexer, parser, visitor (builds DTOs), listener (stats), error collector, and the inline `SemanticValidator`. This is the module the AISafe application actually runs (US121 pilot console → `FlightDslCompiler`). |
| `aisafe/aisafe.flightdsl-semantic-validator` | **A standalone LPROG semantic validator** (package `lapr4.lprog.flightdsl`): a self-contained AST + rule-object implementation of the §3.4.5 rules, exercised by its own unit tests. It is **not** on the application's runtime path (nothing outside the module references it); it stands alone as the LPROG-course rule-pattern deliverable. See §6.1. |

---

## 2. Internal Representation (AST)

After a successful parse, `FlightDomainBuildingVisitor` walks the parse tree and builds
a tree of immutable record types in `lapr4.lprog.flightdsl.ast`:

```
FlightPlanFile
└── Flight  (1..*)
    ├── identifier : String          — e.g. "TP123"
    ├── type : FlightType            — REGULAR | CHARTER
    ├── route : Route
    │   ├── origin : AirportRef
    │   └── destination : AirportRef
    └── legs : List<Leg>  (1..*)
        ├── number : int
        ├── departure : Departure
        │   ├── airport : AirportRef
        │   └── when : TemporalLiteral
        ├── arrival : Arrival
        │   ├── airport : AirportRef
        │   └── when : TemporalLiteral
        ├── fuel : Fuel
        │   ├── quantity : double
        │   └── unit : FuelUnit      — KG | L
        └── segments : List<Segment>  (1..*)
            ├── start : Coordinate   — (latitude, longitude)
            ├── end : Coordinate
            ├── slots : List<Slot>  (1..*)
            │   ├── altitude : Length  — metres
            │   └── width : Length
            └── wind : Wind
                ├── direction : double  — degrees [0, 360)
                └── speed : double      — m/s

AirportRef  : code : String              — IATA (3 letters) or ICAO (4 letters)
TemporalLiteral : raw : String           — as written in source
              value : LocalDateTime      — null if calendar-invalid
Position    : line : int, column : int  — source location for error reporting
```

Every AST node carries a `Position` so that error messages include line and column
(requirement §3.4.4).

---

## 3. Semantic Rules (§3.4.5)

Rules are grouped into six families. In the **standalone LPROG validator** (§6.1) each
family is a `SemanticRule` class emitting the `SemanticErrorCode` enum constants shown
in the *Error constant* column below; all rules run in a single pass and write to a
shared `ErrorCollector` (no rule aborts early), and the findings are returned in
`SemanticValidationResult`.

The **wired pipeline** in `aisafe.flightplandsl` (the one the app runs) enforces the
same rules but emits the short string codes `S1`, `S5`–`S11`, `S14`–`S17`; the
distribution differs slightly because some rules are grammar-enforced at parse time
(see §6.3). Where a code is only produced by one of the two, it is noted in the tables.

### 3.1 Flight-Level Rules (`FlightLevelRule`)

| Code | Description | Error constant |
|---|---|---|
| S1 | Flight identifier must be unique within the file. | `FLIGHT_ID_NOT_UNIQUE` |
| S2 | Flight type must be `REGULAR` or `CHARTER` (null type is invalid). | `FLIGHT_TYPE_INVALID` |

### 3.2 Leg-Level Rules (`LegLevelRule`)

| Code | Description | Error constant |
|---|---|---|
| S3 | Each flight must have at least one leg. | `FLIGHT_MISSING_LEGS` |
| S4a | Each leg must have a departure block. | `LEG_MISSING_DEPARTURE` |
| S4b | Each leg must have an arrival block. | `LEG_MISSING_ARRIVAL` |
| S4c | Each leg must have at least one segment. | `LEG_MISSING_SEGMENTS` |
| S4d | Each leg must have a fuel block. | `LEG_MISSING_FUEL` |
| S5 | Fuel quantity must be strictly positive. | `FUEL_NOT_POSITIVE` |

> Rules S3/S4 are grammar-enforced at parse time. They are re-checked by `LegLevelRule`
> as a defensive measure when the AST is constructed programmatically (e.g. in tests).

### 3.3 Leg-Sequence Coherence (`LegSequenceRule`)

| Code | Description | Error constant |
|---|---|---|
| S6 | Arrival airport of leg `n` must equal the departure airport of leg `n+1`. | `LEG_SEQUENCE_AIRPORT_MISMATCH` |
| S7 | Arrival datetime of leg `n` must be strictly before the departure datetime of leg `n+1`. | `LEG_SEQUENCE_TIME_NOT_ORDERED` |

### 3.4 Route Coherence (`RouteCoherenceRule`)

| Code | Description | Error constant |
|---|---|---|
| S8 | Route `origin` must equal the departure airport of the first leg. | `ROUTE_ORIGIN_MISMATCH` |
| S9 | Route `destination` must equal the arrival airport of the last leg. | `ROUTE_DESTINATION_MISMATCH` |
| S10 | An airport may not be visited more than once within a flight. | `ROUTE_AIRPORT_REPEATED` |

### 3.5 Segment Coherence (`SegmentCoherenceRule`)

| Code | Description | Error constant |
|---|---|---|
| S11 | Segment `start` and `end` coordinates must be different points. | `SEGMENT_START_EQUALS_END` |

### 3.6 Domain Consistency (`DomainConsistencyRule`)

| Code | Description | Error constant |
|---|---|---|
| S12 | Airport codes must conform to IATA (exactly 3 uppercase letters) or ICAO (exactly 4 uppercase letters). | `AIRPORT_CODE_BAD_FORMAT` (standalone only; grammar-enforced in the wired pipeline) |
| S13/S14 | Slot altitude, slot width and wind speed must be strictly positive. (Fuel quantity is covered separately by S5 / `FUEL_NOT_POSITIVE`, not here.) | `NUMERIC_NOT_POSITIVE` (standalone) / `S14` (wired pipeline) |
| S15 | Wind direction must be in the range `[0, 360)`. | reported by the wired-pipeline `SemanticValidator` as `S15` (no standalone-module equivalent) |
| S16 | Latitude must be in `[-90, 90]`; longitude must be in `[-180, 180]`. | reported by the wired-pipeline `SemanticValidator` as `S16` (no standalone-module equivalent) |
| S17 | Datetime values must represent valid calendar instants (e.g. `2026-02-30T08:00` is invalid). | `DATETIME_INVALID_CALENDAR` (standalone) / `S17` raised by the visitor (wired pipeline) |

---

## 4. Error Reporting

Each semantic finding is a `SemanticError` record:

```
SemanticError
├── code : SemanticErrorCode   — enum constant identifying the rule
├── message : String           — human-readable description with context (airport code, leg number, etc.)
└── position : Position        — source line and column; Position.UNKNOWN if not applicable
```

Rendered format (one error per line):

```
FLIGHT_ID_NOT_UNIQUE @ line 12, column 1: Flight identifier 'TP123' is declared 2 times in this file; identifiers must be unique.
FUEL_NOT_POSITIVE @ line 27, column 10: Flight 'TP456': leg 1 fuel quantity must be strictly positive.
```

`SemanticValidationResult` provides:
- `isValid()` — true iff there are zero findings.
- `has(code)` — true iff at least one finding with that code exists.
- `count(code)` — number of findings with that code.
- `render()` — all findings joined by newline.

---

## 5. Reference Fixtures

Test `.flight` files live in:

`aisafe/aisafe.flightplandsl/src/test/resources/dsl/`

### 5.1 Valid Samples

| File | Purpose |
|---|---|
| `valid_simple.flight` | Single-leg direct flight. |
| `valid_simple_iata.flight` | Single-leg flight using IATA (3-letter) airport codes. |
| `valid_simple_liters.flight` | Single-leg flight with fuel expressed in litres (`L`). |
| `valid_multileg.flight` | Two-leg flight with a stopover. |
| `valid_multisegment.flight` | Single-leg flight with multiple segments. |

### 5.2 Invalid Samples — Semantic Rules

| File | Violates | Description |
|---|---|---|
| `invalid_S1_duplicate_flight_id.flight` | S1 | Two `flight TP123 { … }` blocks in the same file. |
| `invalid_S5_negative_fuel.flight` | S5 | `fuel: -100 kg`. |
| `invalid_S6_leg_airport_mismatch.flight` | S6 | Leg 1 arrives at `LPPT`, leg 2 departs from `LEMD`. |
| `invalid_S7_leg_time_overlap.flight` | S7 | Leg 2 departs before leg 1 arrives. |
| `invalid_S8_route_origin_mismatch.flight` | S8 | Route origin does not match first leg departure. |
| `invalid_S9_route_destination_mismatch.flight` | S9 | Route destination does not match last leg arrival. |
| `invalid_S10_airport_visited_twice.flight` | S10 | The same airport appears twice as a stop. |
| `invalid_S11_segment_zero_length.flight` | S11 | `start` equals `end`. |
| `invalid_S11_same_latitude.flight` | S11 | `start` and `end` share a latitude but differ in longitude (must still be treated as different points — negative case). |
| `invalid_S14_negative_altitude.flight` | S14 | Slot altitude is a negative value. |
| `invalid_S14_negative_slot_width.flight` | S14 | Slot width is a negative value. |
| `invalid_S14_negative_wind_speed.flight` | S14 | Wind speed is a negative value. |
| `invalid_S15_wind_direction_out_of_range.flight` | S15 | Wind direction is `400`. |
| `invalid_S15_wind_direction_negative.flight` | S15 | Wind direction is negative. |
| `invalid_S16_bad_latitude.flight` | S16 | Latitude is above `90`. |
| `invalid_S16_latitude_low.flight` | S16 | Latitude is below `-90`. |
| `invalid_S16_bad_longitude.flight` | S16 | Longitude is above `180`. |
| `invalid_S16_longitude_low.flight` | S16 | Longitude is below `-180`. |
| `invalid_S17_bad_date.flight` | S17 | Datetime `2026-02-30T08:00` is not a valid calendar date. |
| `invalid_multi_errors.flight` | S5, S6, S15 | Combines three independent violations to verify collect-all behaviour. |

### 5.3 Invalid Samples — Lexical / Syntactic

| File | Violates | Description |
|---|---|---|
| `invalid_lexical_lowercase_iata.flight` | Lexical | Lowercase airport code `lis` fails the `IATA_CODE` token rule. |
| `invalid_syntax_missing_brace.flight` | Syntactic | Missing `}` after a leg block. |

---

## 6. Implementation Notes

### 6.1 The wired pipeline vs. the standalone LPROG validator

Semantic analysis on the **application's runtime path** is performed entirely by the
inline `SemanticValidator` in `aisafe.flightplandsl` (as part of `FlightDslCompiler`,
over the DTO types built by the visitor) plus the calendar-date check (`S17`) raised
by `FlightDomainBuildingVisitor`. This is the EAPLI / Java integration the AISafe app
actually invokes (US121 pilot console).

`aisafe.flightdsl-semantic-validator` (`lapr4.lprog.flightdsl`) is a **standalone**
implementation of the same §3.4.5 rule set as a rule-object pattern over its own AST.
It is self-contained — it has no lexer, parser, visitor or listener and is fed only by
its own unit-test fixtures — and is **not** wired into the runtime (no module depends
on it). It stands alone as the LPROG-course exhibit of the rule-pattern design; the two
implementations share the collect-all strategy but are independent codebases.

### 6.2 Fail-fast between phases

`FlightDslCompiler` skips semantic analysis when lexical or syntactic errors are present.
A broken parse tree would produce misleading "missing node" findings that are artefacts
of the broken input, not true semantic violations.

### 6.3 Grammar-enforced vs. semantically-enforced rules

Some rules that appear semantic are already enforced by the grammar (e.g. S2: the grammar
requires `TYPE COLON value=(REGULAR | CHARTER)` so an invalid type cannot be parsed).
The semantic validator re-checks these programmatically to support test scenarios where
AST nodes are constructed directly without going through the parser.