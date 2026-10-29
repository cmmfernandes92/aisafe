# Base Project Decisions

## Context

This document records the decisions made before committing the base project structure, as required by Section 5 of the Application Engineering Process. The team started from the course's `eapli.base` template (not included in this public repository) and forked it into a new multi-module project named `aisafe`. The example *utentes* domain shipped with `eapli.base` was not carried over; only framework code was.

## Module Structure

Flat multi-module layout inherited from `eapli.base`. All modules share parent `groupId=eapli`, `artifactId=aisafe`, version `1.0.0-SNAPSHOT`. The `eapli` `groupId` is preserved to maintain alignment with the framework lineage.

| Module | Role |
|---|---|
| `aisafe.infrastructure.application` | Application lifecycle, settings, authentication infrastructure |
| `aisafe.core` | Domain layer and application services |
| `aisafe.bootstrappers` | Per-feature bootstrappers and smoke-test runner |
| `aisafe.persistence.impl` | Repository implementations (JPA and in-memory) |
| `aisafe.app.common.console` | Shared console UI (BaseApp, login/logout/change-password) |
| `aisafe.app.admin.console` | Admin app |
| `aisafe.app.backoffice.console` | Backoffice Operator app |
| `aisafe.app.atcc.console` | Air Transport Company Collaborator app |
| `aisafe.app.pilot.console` | Pilot app |
| `aisafe.app.fco.console` | Flight Control Operator app |
| `aisafe.app.weather.console` | Weather Person app |
| `aisafe.app.bootstrap` | Bootstrap entry point (seeds the database) |

### Build Configuration

Console app jars do not declare a main class via `maven-jar-plugin`. Apps are
launched through the provided run scripts, which specify the main class on
the command line via `-cp`. This mirrors the eapli.base convention and
avoids an unpinned-plugin warning at build time.

## Logical Architecture

Four-layer architecture inherited from `eapli.base`:

- **Presentation** - console UIs (`aisafe.app.*.console`)
- **Application** - use-case controllers coordinating domain objects
- **Domain** - aggregates, entities, value objects, domain services (`aisafe.core`)
- **Persistence** - repository implementations (`aisafe.persistence.impl`)

Repository interfaces live in the domain layer; implementations live in the persistence layer. Application controllers depend on interfaces, never on implementations.

## Actor-to-App Mapping

One console application per actor identified in the requirements (§3.1):

- **Admin console** - Admin. Registers, enables/disables, and lists users with system access (Backoffice Operators, Flight Control Operators, and others). Scope is user lifecycle management.
- **Backoffice console** - Backoffice Operator. Manages reference data: airports, air control areas, aircraft models, engine models, airlines, collaborators, pilots. Scope is data entry for base system information.
- **ATCC console** - Air Transport Company Collaborator. Creates flights for their own company.
- **Pilot console** - Creates and validates flight plans.
- **FCO console** - Flight Control Operator. Runs simulations, redirects flights, monitors air traffic.
- **Weather console** - Weather Person. Manages weather data.

## Package Organization

Feature-based packaging within each module: `eapli.aisafe.<feature>.domain`, `eapli.aisafe.<feature>.application`, `eapli.aisafe.<feature>.repositories`. Feature packages are created per-US as each feature is first implemented; the base commit ships empty module skeletons only.

## Naming Conventions

- **Controllers** - `<Verb><Entity>Controller` (e.g., `RegisterFlightController`, `ListFlightsController`, `RedirectFlightController`)
- **Repository interfaces** - bare entity name (e.g., `FlightRepository`)
- **Repository implementations** - `Jpa<Entity>Repository` and `InMemory<Entity>Repository`
- **Value Objects** - no suffix; the class name alone indicates the role
- **UI classes** - `<Verb><Entity>UI` matching the controller
- **Actions** - `<Verb><Entity>Action` for menu-bound commands

## Bootstrap Strategy

Bootstrappers are added per-US, committed alongside the US that introduces the entity. The `aisafe.bootstrappers` module ships empty in the base commit; each developer adds their bootstrapper when implementing their US. Each bootstrapper uses the same controller as its US so invariants are enforced uniformly.

Seeding dependency order (relevant for the smoke test and sprint planning):

1. Admin user
2. Backoffice users (US031)
3. Air control areas (US050)
4. Airports (US052)
5. Aircraft engine models (US056)
6. Aircraft models (US055)
7. Air transport companies (US060)
8. Airline collaborators (US061)
9. Pilots, routes, and sample flight/flight plan for end-to-end verification