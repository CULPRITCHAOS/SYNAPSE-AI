# REPO_MODULE_SCAFFOLDING_V0

## Purpose

This document fixes the initial Gradle/module structure for `SYNAPSE-AI`.

The goal is to make the repo coding-ready without over-modularizing it into useless complexity.

This scaffolding is designed around current Android architecture guidance:
- layered app structure
- UI state owned by state holders
- repositories as data-layer entry points
- single source of truth per data type
- feature modularization using `api` / `impl` splits where that separation is useful
- multi-module dependency management through version catalogs and convention plugins/build logic

---

## Foundation

This module plan is based on these current Android docs:

1. The Android app architecture guide says the UI layer is made of UI elements plus **state holders**, and those state holders typically depend on the **data layer**, with an optional domain layer when it helps. It also says the data layer is made of **repositories**, and you should create a repository class for each different type of data your app handles. citeturn512703search7turn512703search3turn512703search2

2. Android’s UI guidance recommends **unidirectional data flow** and state holders (`ViewModel` or plain class depending on lifecycle/scope) for UI state production. citeturn512703search3turn512703search2

3. Android’s newer modularization guidance for navigation says each feature can be split into `api` and `impl` submodules, with navigation keys/contracts in `api` and implementation content/entry providers in `impl`. citeturn512703search0turn512703search1

4. Android build guidance recommends using **version catalogs** for multi-module projects with shared dependencies, and recommends keeping build files declarative while moving build logic into plugins. citeturn503850search0turn503850search1turn503850search7

5. Android DI guidance says Hilt works well in regular multi-module Gradle projects, while dynamic feature module setups can require more Dagger-specific patterns. citeturn503850search2turn503850search5turn503850search3

Implication:

- Synapse should start as a regular multi-module Android app, not a dynamic-feature puzzle.
- Features should use `api` / `impl` splits where cross-feature navigation/contracts matter.
- Data/state ownership should stay centralized behind repositories.
- Build conventions and dependency versions should be centralized early.

---

## Scaffolding Principles

### 1. Separate contracts from implementations
Anything other modules need to depend on should live in a small contract module, not in a feature implementation module.

### 2. Keep one source of truth per data type
State, app registry, tool registry, audit receipts, and model registry should each have clear ownership.

### 3. Use pure Kotlin modules where Android is not required
Contracts, orchestration state machines, eval types, and validation logic should stay as platform-light as possible.

### 4. Do not over-modularize
If a module has no real ownership boundary, do not create it just to look sophisticated.

### 5. App module is composition root
The host app module wires DI, startup, navigation shell, and top-level runtime composition.

---

## Recommended Top-Level Structure

```text
SYNAPSE-AI/
  app/
    mobile-host/

  build-logic/

  core/
    common/
    model-api/
    tool-api/
    apppack-api/
    event-api/
    device-profile/
    model-registry/
    runtime-governor/
    orchestrator/
    security/
    storage/
    evals/

  runtime/
    local-service/
    provider-fake/
    provider-mediapipe/
    provider-litert/
    provider-openai/
    provider-gemini/

  sdk/
    synapse-client-sdk/

  feature/
    chat/
      api/
      impl/
    connected-apps/
      api/
      impl/
    routines/
      api/
      impl/
    tool-studio/
      api/
      impl/
    perception/
      api/
      impl/
    settings/
      api/
      impl/
    academy/
      api/
      impl/

  integration/
    demo-game-app/
    demo-productivity-app/

  gradle/
    libs.versions.toml
```

---

## Exact Module Responsibilities

## app/mobile-host

### Role
Main Android app module and composition root.

### Owns
- `Application` class
- DI root/container bootstrap
- top-level navigation shell
- app startup / initialization
- runtime service startup wiring
- feature assembly

### Depends on
- all feature `api` modules
- selected feature `impl` modules needed by assembly
- `core/*`
- `runtime/*`

### Must not own
- provider-specific logic
- repository implementations beyond composition
- orchestration business rules

---

## build-logic

### Role
Centralized convention plugins / shared build logic.

### Owns
- Android library/application conventions
- Kotlin conventions
- test conventions
- Compose conventions if used
- lint/formatting/test task conventions

### Why
Android build guidance recommends keeping build files declarative and moving shared build logic into plugins. citeturn503850search7turn503850search1

### Must include
- convention plugin for Android library modules
- convention plugin for Android application modules
- convention plugin for pure Kotlin modules

---

## core/common

### Role
Pure Kotlin primitives shared everywhere.

### Owns
- IDs
- result wrappers
- failure/error types
- clocks/time abstractions
- small serialization helpers
- common enums that are not domain-specific to a single subsystem

### Must remain
- Android-free where possible
- tiny and boring

---

## core/model-api

### Role
Canonical provider interface and provider-facing request/result types.

### Owns
- `ModelProvider`
- `ProviderCapabilities`
- `GenerateRequest`
- `GenerateResult`
- `StreamEvent`
- benchmark/token-counting request/result types

### Depends on
- `core/common`
- optionally contract modules needed for normalized tool/resource descriptors

---

## core/tool-api

### Role
Canonical tool contract and execution result types.

### Owns
- `ToolDeclaration`
- `ToolContract`
- `ToolBinding`
- validation result types
- tool execution request/result contracts

### Depends on
- `core/common`

---

## core/apppack-api

### Role
Canonical connected-app registration contract.

### Owns
- `AppPackV0`
- app capability request types
- Android integration spec types
- compatibility/auth/signature placeholders

### Depends on
- `core/common`
- schema/helper types as needed

---

## core/event-api

### Role
Canonical event/state/resource bus contracts.

### Owns
- `EventEnvelope`
- `StateSnapshotRecord`
- `ResourceRecord`
- subscriptions, cursors, sessions, retention types

### Depends on
- `core/common`

---

## core/device-profile

### Role
Device capability profiling for routing and runtime governance.

### Owns
- `DeviceProfile`
- `DeviceInferenceTier`
- device benchmark receipt types

### Depends on
- `core/common`
- `core/model-api`

---

## core/model-registry

### Role
Inventory and ranking surface for providers/models.

### Owns
- provider/model records
- install/health state
- routing candidate ranking rules

### Depends on
- `core/common`
- `core/model-api`
- `core/device-profile`

---

## core/runtime-governor

### Role
Thermal/battery/failure-loop governor for local AI runtime behavior.

### Owns
- runtime condition types
- governor decisions
- governor receipts

### Depends on
- `core/common`
- `core/device-profile`
- `core/model-registry`

---

## core/security

### Role
Private-build minimum hardening and execution gates.

### Owns
- trust levels
- confirmation levels
- permission requirement types
- execution gate result types
- security receipts

### Depends on
- `core/common`
- `core/tool-api`
- `core/apppack-api`

---

## core/storage

### Role
Data-layer implementation and repositories.

### Owns
- Room entities/DAOs where needed
- DataStore preferences/proto stores where needed
- repositories for app registry, model registry, audit logs, routines, state snapshots, event log, etc.

### Why
Android guidance says repositories are the entry point to the data layer and each data type should have a single source of truth. citeturn512703search7turn512703search3

### Must not leak
- raw DAO usage into orchestrator/UI/features

---

## core/orchestrator

### Role
Host-controlled orchestration loop.

### Owns
- orchestration input types
- loop state machine
- context assembly
- provider selection use of model registry
- tool-plan execution sequencing
- synthesis / fallback / confirmation branches

### Depends on
- `core/common`
- `core/model-api`
- `core/tool-api`
- `core/apppack-api`
- `core/event-api`
- `core/device-profile`
- `core/model-registry`
- `core/runtime-governor`
- `core/security`
- `core/storage`

---

## core/evals

### Role
Eval datasets, graders, metrics, thresholds, receipts.

### Owns
- eval family enums/types
- dataset references
- grader contracts
- eval run receipts
- benchmark receipt adapters

### Depends on
- `core/common`
- contract modules needed for scenario typing

---

## runtime/local-service

### Role
Private inference runtime boundary inside the host app.

### Owns
- bound service / binder interface
- provider lifecycle coordination
- cancellation / health checks
- execution bridge from app host to concrete providers

### Why
Android recommends Binder for same-app same-process bound services. citeturn503850search7turn503850search2

### Hard rule
Third-party apps do **not** bind directly here.

---

## runtime/provider-fake

### Role
Deterministic provider for unit/integration/eval work.

### Must be available early
Yes. This is the first provider to implement.

---

## runtime/provider-mediapipe

### Role
Primary on-device Android provider.

### Depends on
- `core/model-api`
- runtime local-service hooks

---

## runtime/provider-litert

### Role
Accelerator-focused on-device provider.

### Depends on
- `core/model-api`
- runtime local-service hooks

---

## runtime/provider-openai
## runtime/provider-gemini

### Role
Optional remote providers for comparison/fallback.

### Must remain
- thin adapters behind `ModelProvider`
- isolated from feature/UI modules

---

## sdk/synapse-client-sdk

### Role
Developer SDK for apps that want to integrate with Synapse.

### Owns
- client registration helpers
- AppPack builder/validator helpers
- event/resource/state publishing helpers
- IPC/client bridge helpers

### Depends on
- `core/apppack-api`
- `core/event-api`
- selected safe IPC abstractions

---

## feature/*/api and feature/*/impl

### Role
Feature-facing UI contracts and implementations.

### Why
Android’s current navigation modularization guidance explicitly recommends splitting features into `api` and `impl`, with contracts/navigation keys in `api` and implementation content/providers in `impl`. citeturn512703search0

### Pattern
For each feature:
- `api` owns route keys, public feature entry contract, minimal state/command interfaces if needed
- `impl` owns screens, state holders, internal feature wiring, feature-specific use cases

### Initial feature set
- `chat`
- `connected-apps`
- `routines`
- `tool-studio`
- `perception`
- `settings`
- `academy`

### Notes on state holders
Android recommends `ViewModel` for screen-level state holders with data-layer access, while plain classes are fine for more local UI-logic state holders. citeturn512703search3turn512703search2

---

## integration/demo-game-app
## integration/demo-productivity-app

### Role
Sample Synapse-compatible apps used to prove the platform.

### Must not become
- dumping grounds for production host code
- hidden sources of truth

These are reference integrations, not architecture owners.

---

## Dependency Rules

## Allowed dependency direction

```text
app/mobile-host
  -> feature/*/api
  -> feature/*/impl
  -> core/*
  -> runtime/*

feature/*/impl
  -> feature/*/api
  -> core/*

feature/*/api
  -> core/common (and tiny contract modules only if necessary)

runtime/*
  -> core/model-api
  -> core/common
  -> selected contract modules only

sdk/*
  -> core/apppack-api
  -> core/event-api
  -> core/common

core/orchestrator
  -> other core contract/data modules

core/storage
  -> core contract modules

core contract modules
  -> core/common only where possible
```

## Forbidden dependencies

### Feature impl to feature impl
Do **not** let `feature/chat/impl` depend directly on `feature/connected-apps/impl`.
Use `api` contracts or orchestrator/data abstractions instead.

### UI/features to provider implementations
No feature/UI module may depend directly on `provider-openai`, `provider-gemini`, `provider-mediapipe`, or `provider-litert`.

### Orchestrator to feature impl
The orchestrator is not a UI module.

### Runtime providers to storage/UI
Providers should not read Room/DataStore directly or know about feature screens.

### SDK to host internals
Client SDK must not depend on host-only implementation modules.

---

## Source of Truth Ownership

These owners should be fixed early:

### Conversation state
Owner: `core/storage`
Accessed via repository by orchestrator and chat feature.

### Tool registry
Owner: `core/storage` + `core/tool-api`

### App registry / AppPack activation state
Owner: `core/storage` + `core/apppack-api`

### Event log / state snapshots
Owner: `core/storage` + `core/event-api`

### Model registry / benchmark receipts
Owner: `core/model-registry` backed by `core/storage`

### Runtime condition / governor receipts
Owner: `core/runtime-governor` backed by `core/storage`

### UI state
Owner: feature `impl` state holders (`ViewModel` or plain class depending on scope). Android recommends `ViewModel` for screen-level state with data-layer access. citeturn512703search3turn512703search2

---

## Build Conventions

## Version catalog
Use:

```text
gradle/libs.versions.toml
```

Why:
Android recommends version catalogs for shared dependencies across multiple modules. citeturn503850search0turn503850search1

## Convention plugins
Use `build-logic/` for shared Gradle conventions.

Recommended convention plugins:
- `synapse.android.application`
- `synapse.android.library`
- `synapse.kotlin.library`
- `synapse.android.compose`
- `synapse.testing`

Why:
Android build guidance recommends keeping build files declarative and moving build logic into plugins. citeturn503850search7

## Build files
Module build files should mostly declare:
- plugin aliases
- namespace
- Android config
- dependencies

Avoid burying custom build logic in random module `build.gradle.kts` files.

---

## Dependency Injection Strategy

### V0 recommendation
Use Hilt in the host app / normal modules where it fits, but do **not** contort the repo around dynamic feature module DI complexity unless you actually adopt Android dynamic feature modules.

Why:
Android’s guidance says Hilt works well in regular multi-module Gradle projects; deeper dynamic feature arrangements can require more Dagger-specific patterns. citeturn503850search2turn503850search5turn503850search3

### Practical rule
- app module = DI composition root
- feature impl modules receive dependencies via DI
- pure core contract modules stay DI-agnostic

---

## Initial Module Creation Order

### Step 1: build foundation
Create:
- `build-logic/`
- `gradle/libs.versions.toml`
- `app/mobile-host`
- `core/common`

### Step 2: contract modules
Create:
- `core/model-api`
- `core/tool-api`
- `core/apppack-api`
- `core/event-api`

### Step 3: control/data modules
Create:
- `core/device-profile`
- `core/model-registry`
- `core/runtime-governor`
- `core/security`
- `core/storage`
- `core/orchestrator`

### Step 4: runtime adapters
Create:
- `runtime/local-service`
- `runtime/provider-fake`
- then real providers later

### Step 5: first feature shells
Create:
- `feature/chat/api`
- `feature/chat/impl`
- `feature/connected-apps/api`
- `feature/connected-apps/impl`
- `feature/settings/api`
- `feature/settings/impl`

### Step 6: SDK and demos
Create:
- `sdk/synapse-client-sdk`
- `integration/demo-game-app`
- `integration/demo-productivity-app`

This order gives you a usable spine without opening every future module at once.

---

## What Not to Do

1. Do not let every feature become its own source of truth.
2. Do not let provider-specific types leak into the orchestrator or UI.
3. Do not let features depend directly on each other’s implementation modules.
4. Do not create a domain layer by reflex; add it only if it simplifies reuse/complexity.
5. Do not create 40 modules before the first vertical slice works.
6. Do not put real business logic into build scripts.

---

## Definition of Done

`REPO_MODULE_SCAFFOLDING_V0.md` is implementation-ready when:
- top-level module tree is fixed
- each module has a clear owner/responsibility
- dependency direction rules are fixed
- source-of-truth ownership is fixed
- build convention strategy is fixed
- module creation order is fixed

If those are true, the repo is ready to start real scaffolding work instead of arguing about where everything should live.
