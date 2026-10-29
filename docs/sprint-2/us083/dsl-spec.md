# Flight DSL Specification

## 1. Introduction

The Flight DSL is a textual language for defining flight plans in a precise, machine-validatable format. It is used by the AlSafe system to import flight schedules and associated route data.

This document describes the complete lexical, syntactic, and semantic rules of the language implemented in:

`aisafe/aisafe.flightplandsl/src/main/antlr4/eapli/aisafe/flightplanmanagement/dsl/grammar/FlightDSL.g4`

### 1.1 Design Principles

- **Block-based and hierarchical** - mirrors the conceptual model: a file contains one or more flights, each flight contains legs, each leg contains segments.
- **Curly braces** for blocks, `:` for property assignment, commas only inside list/tuple literals.
- **Free whitespace** and line breaks; `// line` and `/* block */` comments are allowed.
- **Keywords are case-insensitive** (for example, `flight`, `FLIGHT`, `Flight`).
- **Literals** (airport codes, flight IDs) are case-sensitive and follow industry conventions (uppercase).
- **Units are explicit** - every physical quantity is followed by a unit token (`kg`, `l`, `m`, `m/s`).

---

## 2. Lexical Structure

### 2.1 Keywords

Keywords are case-insensitive:

```text
flight, type, regular, charter, route, origin, destination,
leg, departure, arrival, airport, datetime, fuel, segments,
segment, start, end, slots, slot, altitude, width, wind,
direction, speed
```

### 2.2 Unit Keywords

Unit keywords are case-insensitive:

| Token | Meaning |
|---|---|
| `kg` | kilograms |
| `l` | litres |
| `m` | metres |
| `m/s` | metres per second |

### 2.3 Literal Tokens

Literal tokens are case-sensitive:

| Token | Pattern | Example |
|---|---|---|
| Flight ID | `[A-Z]{2}[0-9]{1,4}[A-Z]?` | `TP123`, `LH4205`, `AF77A` |
| IATA code | `[A-Z]{3}` | `LIS`, `JFK`, `OPO` |
| ICAO code | `[A-Z]{4}` | `LPPT`, `KJFK`, `LPPR` |
| Datetime literal | `YYYY-MM-DD'T'HH:MM(:SS)?` | `2026-05-20T08:30` |
| Integer | `-?[0-9]+` | `100`, `-50` |
| Float | `-?[0-9]+\.[0-9]+` | `38.78`, `-9.13` |

### 2.4 Punctuation

```text
{ } ( ) [ ] , :
```

### 2.5 Comments

- Line comment: `//` until end of line.
- Block comment: `/* ... */` and may span multiple lines.

Whitespace (spaces, tabs, line breaks) is ignored outside tokens.

---

## 3. Syntactic Specification

A flight plan file consists of one or more `flight` blocks and then end of file.

### 3.1 Overall Structure

```text
flightPlanFile ::= flight+ EOF

flight ::= 'flight' FLIGHT_ID '{' flightType route leg+ '}'
```

### 3.2 Flight Type

```text
flightType ::= 'type' ':' ('regular' | 'charter')
```

### 3.3 Route

```text
route ::= 'route' '{'
            'origin' ':' airportCode
            'destination' ':' airportCode
          '}'
```

`airportCode` can be either an IATA (3 letters) or ICAO (4 letters) code.

### 3.4 Leg

```text
leg ::= 'leg' INT '{' departure arrival fuel segmentsBlock '}'

departure ::= 'departure' '{'
                'airport' ':' airportCode
                'datetime' ':' DATETIME_LIT
              '}'

arrival ::= 'arrival' '{'
              'airport' ':' airportCode
              'datetime' ':' DATETIME_LIT
            '}'

fuel ::= 'fuel' ':' number ('kg' | 'l')

segmentsBlock ::= 'segments' '{' segment+ '}'
```

### 3.5 Segment

```text
segment ::= 'segment' '{'
              'start' ':' coordinates
              'end' ':' coordinates
              slotsBlock
              wind
            '}'

coordinates ::= '(' number ',' number ')'

slotsBlock ::= 'slots' '{' slot+ '}'

slot ::= 'slot' '{'
           'altitude' ':' length
           'width' ':' length
         '}'

length ::= number 'm'

wind ::= 'wind' '{'
           'direction' ':' number
           'speed' ':' number 'm/s'
         '}'
```

### 3.6 Numbers

`number` can be an integer or a floating-point literal.

### 3.7 Order of Properties

Inside each block the order of properties is fixed as shown above. For example, inside a `leg` block you must write `departure`, then `arrival`, then `fuel`, then `segments`. This makes parsing deterministic and error messages precise.

---

## 4. Semantic Rules

The following rules are enforced during validation. A flight plan that violates any rule is rejected with a clear error message.

### 4.1 Flight Level

| Rule | Description |
|---|---|
| S1 | Flight identifier must be unique within the file. |
| S2 | Flight type must be `regular` or `charter` (enforced by grammar). |

### 4.2 Leg Level

| Rule | Description |
|---|---|
| S3 | Each leg has exactly one departure and one arrival (grammar enforced). |
| S4 | Each leg has at least one segment (grammar enforced). |
| S5 | Fuel quantity must be strictly positive. |

### 4.3 Leg Sequence Coherence

| Rule | Description |
|---|---|
| S6 | Arrival airport of leg `n` must equal the departure airport of leg `n+1`. |
| S7 | Arrival time of leg `n` must be strictly before the departure time of leg `n+1`. |

### 4.4 Route Coherence

| Rule | Description |
|---|---|
| S8 | Route `origin` must equal the departure airport of the first leg. |
| S9 | Route `destination` must equal the arrival airport of the last leg. |
| S10 | An airport may not be visited twice within the flight. |

### 4.5 Segment Coherence

| Rule | Description |
|---|---|
| S11 | Segment `start` and `end` coordinates must be different. |
| S12 | Every segment belongs to exactly one leg (guaranteed by the AST). |

### 4.6 Domain Consistency

| Rule | Description |
|---|---|
| S13 | Airport codes must be valid IATA (3 letters) or ICAO (4 letters). A whitelist may be used when available. |
| S14 | Numeric magnitudes must be strictly positive for fuel, slot altitude, slot width, and wind speed. |
| S15 | Wind direction must be in the range `[0, 360)`. |
| S16 | Latitude must be in `[-90, 90]`; longitude must be in `[-180, 180]`. |
| S17 | Datetime values must be valid calendar instants (for example, `2026-02-30` is invalid). |

---

## 5. Reference Fixtures

The current reference DSL files live in:

`aisafe/aisafe.flightplandsl/src/test/resources/dsl/`

### 5.1 Valid Samples

| File | Purpose |
|---|---|
| `valid_simple.flight` | Single-leg direct flight. |
| `valid_multileg.flight` | Two-leg flight with a stopover. |
| `valid_multisegment.flight` | Single-leg flight split into multiple segments. |

### 5.2 Invalid Samples

Each invalid fixture is named after the rule it is meant to exercise:

| File | Violates | Why |
|---|---|---|
| `invalid_S1_duplicate_flight_id.flight` | S1 | Two `flight TP123 { ... }` blocks in the same file. |
| `invalid_S5_negative_fuel.flight` | S5 | `fuel: -100 kg`. |
| `invalid_S6_leg_airport_mismatch.flight` | S6 | Leg 1 arrives at one airport, leg 2 departs from another. |
| `invalid_S7_leg_time_overlap.flight` | S7 | Leg 2 departs before leg 1 arrives. |
| `invalid_S8_route_origin_mismatch.flight` | S8 | Route origin does not match the first leg departure airport. |
| `invalid_S9_route_destination_mismatch.flight` | S9 | Route destination does not match the last leg arrival airport. |
| `invalid_S10_airport_visited_twice.flight` | S10 | The same airport is visited twice within the flight. |
| `invalid_S11_segment_zero_length.flight` | S11 | Segment `start` equals segment `end`. |
| `invalid_S14_negative_altitude.flight` | S14 | Slot altitude is negative. |
| `invalid_S15_wind_direction_out_of_range.flight` | S15 | Wind direction is `400`. |
| `invalid_S16_bad_latitude.flight` | S16 | Latitude is outside `[-90, 90]`. |
| `invalid_S17_bad_date.flight` | S17 | Datetime uses an invalid calendar date: `2026-02-30T08:00`. |
| `invalid_syntax_missing_brace.flight` | Syntax | Missing `}` after a leg block. |
| `invalid_lexical_lowercase_iata.flight` | Lexical | Lowercase airport code: `lis`. |
| `invalid_multi_errors.flight` | S5, S6, S15 | Combines negative fuel, leg-airport mismatch, and bad wind direction. |

---

## 6. Error Reporting

When a flight plan file does not satisfy the lexical, syntactic, or semantic rules, the compiler returns a list of errors. Each error contains:

- Phase - `LEXICAL`, `SYNTACTIC`, or `SEMANTIC`.
- Code - for example, `S5`, `S15`, `LEX`, or `PARSE`.
- Message - human-readable description.
- Location - line number and column where the error was detected.

All semantic errors are collected in a single pass; the validator does not stop at the first semantic error.

---

## 7. Notes on Extensibility

The language is designed to be extended in the future with:

- Per-segment cruise speed and vertical profile.
- Alternate airports for diversion.
- Inline aircraft binding.
- Route templates and macro definitions.

Any extension will be backward-compatible with the core syntax defined in this document.
