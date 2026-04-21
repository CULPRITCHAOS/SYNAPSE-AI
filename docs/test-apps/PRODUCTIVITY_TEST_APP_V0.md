# PRODUCTIVITY_TEST_APP_V0

## Purpose

This document defines the **first controlled co-app test target** for Synapse.

It is not meant to be a production app spec.
It is not meant to lock exact implementation details too early.
It exists to give Synapse a **clean, low-chaos, app-owned integration surface** for validating:

- App Pack registration
- capability grant flows
- tool invocation
- state snapshots
- event ingestion
- audit/security receipts
- basic orchestration across host and co-app boundaries

This is a **north star for co-app testing**, not a detailed coding contract.

---

## Why start with a productivity app

The first co-app should be boring on purpose.

A productivity-style app is the best first target because it:
- has easy-to-understand tools
- has low-risk state mutations
- has obvious event/state separation
- avoids game-specific cheating / abuse ambiguity
- makes contract bugs easier to isolate
- tests whether Synapse can act as a real app brain without depending on wild UI automation

This app should prove that Synapse can reason over a typed app surface and perform useful multi-step work.

---

## Working Name

Use a placeholder name for now:

> **Task Forge**

This can change later.
The important part is the role, not the brand.

---

## Core Product Role

The test app should act like a small local productivity app that exposes:

- a task list
- a note surface
- a focus mode / workflow state
- structured tools
- structured events
- structured state snapshots

Synapse should be able to use the app as a **clean first-party / co-app integration target**.

---

## What this app is for

This app exists to test whether Synapse can do all of the following cleanly:

1. register an app through `AppPack`
2. review and approve requested capabilities
3. discover declared tools without hardcoding app logic
4. call tools through validated contracts
5. ingest app events and state snapshots
6. use current app state in planning/routing
7. emit visible tool/action traces to the user
8. preserve host authority while still being useful

If the app cannot help prove those things, it is not the right first co-app.

---

## What this app is NOT for

This app is not intended to:

- prove advanced UI automation
- prove accessibility-only control
- test adversarial game-control cases first
- validate every possible App Pack field
- become a giant feature-rich productivity platform
- hide weak Synapse contracts behind app-specific hacks

The point is controlled validation, not feature inflation.

---

## First-Class Integration Surfaces

The productivity test app should expose all four major Synapse-facing surfaces:

### 1. Tools
Model-callable actions.

### 2. State snapshots
Bounded current-state views that Synapse can use as context.

### 3. Events
Transient updates that matter over time.

### 4. App metadata / capability requests
Registration and trust boundary surface through `AppPack`.

This makes it the cleanest first app for proving the full app-brain loop.

---

## Suggested Tool Categories

The exact tool list can evolve, but the first app should expose tools from these categories:

### A. Task creation / mutation
Examples:
- create task
- complete task
- reopen task
- change priority
- add due date

### B. Task inspection / retrieval
Examples:
- list tasks
- fetch active tasks
- fetch overdue tasks
- fetch task details

### C. Notes / capture
Examples:
- create note
- append note
- summarize note surface

### D. Focus / workflow state
Examples:
- enable focus mode
- disable focus mode
- switch project/workspace

### E. Optional low-risk workflow helper
Examples:
- create task from note
- convert task to reminder
- archive completed items

### Hard rule
The first tool set should stay:
- typed
- bounded
- inspectable
- understandable

Avoid generic trash like:
- `do_action`
- `run_workflow`
- `perform`

---

## Suggested State Snapshot Categories

The app should expose a few bounded current-state surfaces.

Suggested categories:

### A. Task summary state
Examples:
- total open tasks
- overdue count
- high-priority count
- current focus task

### B. Active project / workspace state
Examples:
- current project name
- active project task count
- current workflow mode

### C. Focus mode state
Examples:
- focus enabled/disabled
- allowed interruption state
- active session metadata

### D. Optional note summary state
Examples:
- latest notes count
- pinned note summary

### Hard rule
Snapshots should represent **current truth**, not long event history.

---

## Suggested Event Categories

The app should emit a small, useful event set.

Suggested categories:

### A. Task lifecycle events
Examples:
- task created
- task completed
- task updated
- overdue status changed

### B. Workflow state events
Examples:
- focus mode enabled
- focus mode disabled
- project switched

### C. Optional note events
Examples:
- note created
- note pinned

### Hard rule
Events should be:
- schema-defined
- low-noise
- relevant to context assembly
- not spammed for every microscopic UI action

---

## Suggested Capability Request Shape

The app should request only what it actually needs.

Likely first capability classes:
- task management
- note management
- workflow/focus state
- notifications (only if genuinely useful)

It should **not** request:
- broad device control
- screen control
- network access it does not truly need
- random future-facing dangerous capabilities just because they might be useful later

This app should help prove least-privilege patterns, not weaken them.

---

## What Synapse should be able to do with this app

The first meaningful user flows should look like this:

- “Create a task to call Jacob tomorrow.”
- “What’s left on my active project?”
- “Turn on focus mode.”
- “Summarize what I still need to finish today.”
- “Mark the newest task as complete.”
- “Take this note and make it a task.”

Those are strong first flows because they require:
- tool selection
- argument shaping
- current-state use
- event/state updates
- visible result tracing

---

## Why this app matters architecturally

This app should prove that Synapse can function as:

> **a host brain over a typed co-app contract**

without relying on:
- random accessibility hacks
- brittle deep-link guessing
- per-app hardcoded logic
- fake generic tool wrappers

If Synapse cannot handle this app cleanly, it is not ready for more ambitious co-app scenarios.

---

## Security / Trust Expectations

This app should be used to prove the right security posture early.

### Security goals
- registration does not equal trust
- capability requests require approval
- tools remain host-controlled
- high-impact actions still honor confirmation policy
- app state/events remain app-scoped claims, not universal truth
- receipts are emitted for meaningful allow/deny decisions

### Why this matters
This is the safest place to validate your host security architecture before connecting more powerful or game-like apps.

---

## Success Criteria

This test app is successful when all of the following are true:

1. it can be represented cleanly through `AppPack`
2. it exposes tools without schema hacks
3. it exposes state snapshots without overloading event semantics
4. it emits a useful low-noise event stream
5. Synapse can complete at least a few real app-brain workflows using it
6. no per-app hardcoded special-case logic is required in Synapse core
7. capability grants and audit traces remain understandable

---

## Kill Criteria

This test app is failing its purpose if:

- Synapse has to hardcode app-specific orchestration logic
- tool definitions become vague or generic
- event/state boundaries blur into a mess
- registration/capability flows are too sloppy to reason about
- the app hides problems instead of exposing them

If those happen, fix the contracts before adding more app complexity.

---

## Suggested Follow-On Docs

When implementation gets closer, this folder can grow with:

- `PRODUCTIVITY_TEST_APP_APPPACK_DRAFT.json`
- `PRODUCTIVITY_TEST_APP_TOOL_SURFACES.md`
- `PRODUCTIVITY_TEST_APP_STATE_EVENTS.md`
- `PRODUCTIVITY_TEST_APP_SECURITY_NOTES.md`
- `PRODUCTIVITY_TEST_APP_EVAL_CHECKLIST.md`

Do not create all of these yet unless they become necessary.

---

## Bottom Line

The first co-app should be:

- boring enough to debug
- structured enough to prove the contracts
- useful enough to feel real
- low-risk enough to validate security and control boundaries

That is why a small productivity test app is the correct first co-app for Synapse.
