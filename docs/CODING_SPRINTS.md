# CODING_SPRINTS

## Intent

This sprint plan exists to move `SYNAPSE-AI` from concept repo to coding-ready repo without skipping the contracts that keep the architecture sane.

The main rule is:

> Build the contracts before chasing model demos.

That means the early sprints prioritize:
- repo structure
- provider contracts
- app contracts
- policy gates
- benchmark receipts
- orchestration loop

---

## Current Foundation Assumptions

- Android architecture should stay layered: UI + data minimum, optional domain layer where it buys reuse/clarity.
- Feature modules should prefer `api` / `impl` splits where that helps keep boundaries clean.
- On-device Android AI should be treated as real-device work, not emulator-first work.
- S26 Ultra is the primary dev target and should be treated as a strong local-first device, but routing decisions must still be benchmark-driven.

---

## Sprint 0 — Repo Foundation

### Goal
Establish a repo structure that supports clean contracts and future coding sprints.

### Deliverables
- create base modules
- add `core/common`
- add `core/model-api`
- add `core/tool-api`
- add `core/apppack-api`
- add `core/device-profile`
- add `core/model-registry`
- add `core/security`
- add `core/storage`
- add `runtime/provider-fake`

### Docs required by end of sprint
- `DEVICE_PROFILE_V0.md`
- `MODEL_REGISTRY_V0.md`
- `RUNTIME_GOVERNOR_V0.md`
- `APPPACK_V0.md`
- `MODEL_PROVIDER_V0.md`

### Definition of done
- modules compile
- fake provider compiles
- core contracts compile
- repo boundaries are visible and documented

---

## Sprint 1 — Host Loop Skeleton

### Goal
Create the minimum Synapse orchestration loop.

### Deliverables
- add `core/orchestrator`
- implement request intake
- implement task-profile selection
- implement provider lookup via `ModelRegistry`
- implement security gate before tool execution
- add initial `feature/chat/api`
- add initial `feature/chat/impl`

### Definition of done
- a test request can enter the orchestrator
- fake provider can return a text response
- fake provider can return a tool plan
- security gate is hit before tool execution
- audit log entry is created

---

## Sprint 2 — Device Profiling and Benchmarking

### Goal
Make local-AI routing measurable.

### Deliverables
- implement `DeviceProfile` persistence
- implement benchmark receipt persistence
- add benchmark runner for fake provider + one real provider
- add initial model install-state tracking
- define `DeviceInferenceTier`

### Definition of done
- Synapse can create a device profile
- Synapse can record benchmark receipts
- orchestrator can read device tier
- routing can choose a provider based on device tier and task profile

---

## Sprint 3 — First Real Local Providers

### Goal
Bring up the first actual on-device runtime paths.

### Deliverables
- add `runtime/local-service`
- add `runtime/provider-mediapipe`
- add `runtime/provider-litert`
- add health checks
- add provider failure reporting

### Definition of done
- at least one local provider can load
- at least one local provider can answer a simple request
- at least one local provider can emit a tool plan or structured output
- benchmark receipts can be captured on the real device

### Notes
Use the S26 Ultra as the truth source for:
- latency
- warmup cost
- thermal behavior
- battery behavior

Do not treat emulator output as performance truth.

---

## Sprint 4 — Runtime Governor

### Goal
Stop local-first from becoming a thermal/battery mess.

### Deliverables
- add `core/runtime-governor`
- read thermal/battery/runtime signals
- implement downgrade chain
- implement temporary demotion of unhealthy providers/models
- persist governor receipts

### Definition of done
- governor can downgrade model/provider choice
- heavy paths are reduced under stress
- receipts are visible in logs/dev mode
- repeated failure loops are cut off

---

## Sprint 5 — AppPack Contracts

### Goal
Turn Synapse into a host platform for connected apps.

### Deliverables
- finalize `AppPackV0`
- add `sdk/synapse-client-sdk`
- add app registration flow
- add app capability grant flow
- add app tool/resource/event/state registration

### Definition of done
- one sample app can register
- Synapse can store the AppPack
- Synapse can list app tools/resources
- Synapse can receive at least one event/state update from the app

---

## Sprint 6 — First Vertical Slice: Demo Game App

### Goal
Prove the “Synapse as brain for apps” story with a concrete game example.

### Deliverables
- add `integration/demo-game-app`
- register game tools
- register game events
- register one state snapshot
- drive one user request through the full stack

### Definition of done
- user asks for a game action
- orchestrator loads app context
- provider selects a valid tool
- security gate approves or blocks
- action executes
- result is logged and surfaced

---

## Sprint 7 — Second Vertical Slice: Productivity App

### Goal
Prove the app-brain pattern is not game-only.

### Deliverables
- add `integration/demo-productivity-app`
- task/project tools
- event stream or state snapshots
- one multi-step workflow example

### Definition of done
- productivity app can register
- Synapse can reason over its tools/resources/state
- same orchestration path works without app-specific hacks

---

## Sprint 8 — Evals and Regression Gates

### Goal
Stop model/provider behavior from drifting silently.

### Deliverables
- add `core/evals`
- tool-routing eval set
- policy eval set
- app-pack parsing eval set
- provider-comparison eval set

### Definition of done
- eval suite runs locally
- baseline receipts are stored
- provider regressions are visible
- route/provider changes can be compared using receipts instead of vibes

---

## Hard Rules for Every Sprint

1. No feature may call providers directly from UI.
2. No direct tool execution may bypass security/policy.
3. No app integration may bypass AppPack contracts.
4. No provider may be trusted without health/benchmark data.
5. No sprint should add more providers or features than the current contracts can support cleanly.

---

## Recommended Immediate Next File Set

After this sprint plan, the next docs to write are:
- `APPPACK_V0.md`
- `MODEL_PROVIDER_V0.md`
- `TOOL_CONTRACT_V0.md`
- `EVENT_STATE_BUS_V0.md`
- `ORCHESTRATION_LOOP.md`
- `SECURITY_POLICY_V0.md`
- `EVAL_PLAN_V0.md`

These turn the repo from “vision docs” into “coding-sprint ready docs.”

---

## Governing Principle

> Models are replaceable. Contracts, routing, policy, and receipts are the foundation.

That is the discipline that keeps Synapse from turning into a pile of AI demo hacks.
