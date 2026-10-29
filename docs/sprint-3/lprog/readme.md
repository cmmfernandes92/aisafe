# LPROG — Sprint 3 Integration Notes (US120 + US121)

Where the LPROG work lives inside the `aisafe/` Maven project, and how to build, test and
demonstrate it. 

## Status (verified 2026-06-14)

- `mvn test` on `aisafe.flightplandsl` **passes — 84 tests, 0 failures, BUILD SUCCESS**
  (instruction & branch coverage 100 %, grammar package excluded). This resolves the
  Sprint 2 `maven-compiler-plugin` build failure.
- The standalone rule module `aisafe.flightdsl-semantic-validator` **passes — 117 tests,
  0 failures**, 100 %/100 % coverage.

## Scope and where it is documented

| Story | What it is | Doc |
|---|---|---|
| **US120** | Flight DSL specification + full lexical/syntactic/**semantic** analysis (the DSL producer). Continues US083. | `docs/sprint-3/us120/readme.md`, `docs/sprint-3/us120/dsl-spec.md` |
| **US121** | Pilot imports a `.flight` file into an existing flight; validates via US120, installs the raw DSL as the flight's `VALIDATED` plan (the EAPLI/application integration). Supersedes Sprint 2 US081. | `docs/sprint-3/us121/readme.md` |


## Modules & design

- **US120 — DSL pipeline (the module the app runs):** `aisafe/aisafe.flightplandsl`.
  Grammar → lexer/parser → listener (stats) → visitor (DTO/AST) → semantic validator →
  `CompilationResult`. Classes and file paths: see the US120 class diagram at
  `docs/sprint-3/us120/us120_class_diagram.svg`.
- **US121 — application integration:** `aisafe/aisafe.core` (controller + `Flight`
  aggregate) and `aisafe/aisafe.app.pilot.console` (pilot UI). Classes and flow: see the
  US121 class diagram at `docs/sprint-3/us121/us121_class_diagram.svg`.
- **`aisafe/aisafe.flightdsl-semantic-validator`** (standalone LPROG rule-pattern exhibit,
  package `lapr4.lprog.flightdsl`) is **not** on the runtime path — no module depends on it;
  the compiler the app uses is `aisafe.flightplandsl`. See US120 §6.1.

> Sprint 3 note: the old `flightplanmanagement` aggregate was retired. The import now
> operates on the **`flightmanagement`** `Flight` aggregate (identity = route + departure
> date), with `FlightPlan` as an entity inside it. The Sprint 2 paths
> (`flightplanmanagement.domain.FlightPlan`, `FlightPlanId`, `FlightPlanRepository`,
> `JpaFlightPlanRepository`) no longer exist.

## Build & test commands

Run everything from `aisafe/` (use `./mvnw` on macOS/Linux, `mvnw.cmd` on Windows).

```bash
cd aisafe

# Test the LPROG slice (US120 DSL + US121 integration):
./mvnw -pl aisafe.flightplandsl,aisafe.core,aisafe.app.pilot.console -am test -Dmaven.javadoc.skip=true

# Test just the DSL module (84 tests):
./mvnw -pl aisafe.flightplandsl test

# Package the slice without tests:
./mvnw -pl aisafe.flightplandsl,aisafe.core,aisafe.app.pilot.console -am package -DskipTests -Dmaven.javadoc.skip=true
```

JaCoCo is `0.8.13` and runs cleanly on JDK 21 (bound to the `test` phase) — no
`-Djacoco.skip=true` workaround is needed.

## CLI demo (self-contained — the simplest way to see US120 run)

```bash
cd aisafe
./mvnw -pl aisafe.flightplandsl package -DskipTests -Dmaven.javadoc.skip=true
JAR=aisafe.flightplandsl/target/aisafe.flightplandsl-1.0.0-SNAPSHOT-cli.jar

java -jar "$JAR" aisafe.flightplandsl/src/test/resources/dsl/valid_multileg.flight
java -jar "$JAR" aisafe.flightplandsl/src/test/resources/dsl/invalid_multi_errors.flight
```

Actual output (valid file, exit 0):

```text
Flight DSL validation: OK
File: …/valid_multileg.flight
Flights: 1
Legs: 2
Segments: 2
Airports: LEMD, LPPR, LPPT
AST: 1 flight plan(s) produced
```

Actual output (invalid file, exit 1) — note **all** errors are collected in one pass:

```text
Flight DSL validation: FAILED
File: …/invalid_multi_errors.flight
Errors: 3
[SEMANTIC][S5] line 8:4 leg 1 fuel quantity must be strictly positive
[SEMANTIC][S6] line 19:4 leg 2 departs from LEMD but previous leg arrives at LFPG
[SEMANTIC][S15] line 14:8 wind direction must be in [0, 360)
```

## Console demo (US121 end-to-end)

```bash
cd aisafe
./mvnw package dependency:copy-dependencies -DskipTests -Dmaven.javadoc.skip=true
./run-bootstrap.sh     # seed data — run FIRST   (Windows: run-bootstrap.bat)
./run-pilot.sh         #                          (Windows: run-pilot.bat)
```

Log in as **`ruipedro` / `Password1`** (Rui Pedro), seeded to own a single **`FR401`**
flight (OPO→LIS) left as a fresh `DRAFT` for exactly this demo. In the Pilot menu choose
*Import Flight Plan from File*: the menu lists only that pilot's own flights — select
`FR401`, then give the file path `docs/sprint-3/samples/FR401-OPO-LIS.flight`. The file
passes US120 validation and the plan is promoted `DRAFT → VALIDATED`. For the rejection
path, re-run and import `aisafe.flightplandsl/src/test/resources/dsl/invalid_multi_errors.flight`:
it is rejected with phase/code/line/column messages and nothing is persisted.

> Re-importing a valid file into the same flight exercises the US121 *re-import supersedes*
> path: the previous `VALIDATED` plan is demoted to `DRAFT` (keeping its body) and the new
> content becomes the single `VALIDATED` plan.

## Sprint 2 feedback — what changed in Sprint 3

- **DSL deviations marked as extensions** → US120 readme §3.3 lists every extension
  (case-insensitive keywords, comments, `slots`/`wind` blocks, `m/s`) with rationale; the
  core structure follows the enunciado.
- **"Raw-DSL-only persistence may limit later stories"** → US121 readme §3 keeps the raw
  DSL as a LOB *and* documents the evolution plan (US085 re-parses it for the simulation).
- **Build failure** → resolved; the slice builds and tests green (see Status above).
- **Docs too long** → this note is deliberately short; depth lives in the per-US docs.
- **Multiple formats** → `FlightPlanImporterFactory.supportedExtensions()` is the extension
  point; one format (`.flight`) is implemented today.