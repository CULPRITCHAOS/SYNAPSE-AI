# MODEL_ROUTING_REFACTOR_V1

## Purpose

This document is a **mandatory refactor directive** for the Synapse system.

Synapse is not a single-model chatbot.
Synapse is a:

> local-first, multi-model, future multi-agent Android runtime

This document exists to correct the current implementation direction and force alignment between:

- the docs already written in this repo
- the Android Studio scaffold already started
- the intended Synapse architecture going forward

Android Studio / coding agents should treat this as a **restructure-and-continue** directive, not optional guidance.

---

## Why this document exists now

Implementation has already started.
That is good.
But the first wave of implementation likely began from a simpler mental model:

- one orchestrator
- one model path
- one UI connection
- one execution loop

That is enough to start a smoke scaffold, but it is **not enough to build Synapse correctly**.

If implementation continues without correction, the likely outcome is:

- direct model coupling in the orchestrator
- UI-to-model shortcuts
- provider-specific assumptions leaking into core logic
- no clean path to smaller routing models
- no clean path to future multi-agent behavior

This document exists to stop that drift early while the codebase is still cheap to reshape.

---

## Mandatory instruction to Android Studio / coding agents

Before implementing further model-connected behavior, the coding system must:

1. inspect the current implementation
2. identify all direct model calls or single-model assumptions
3. refactor the runtime so model execution is routed, not hardwired
4. preserve existing progress where possible
5. continue implementation only after routed architecture is in place

This is not a request to throw away the current work.
It is a request to **restructure the current work so future layers do not rot**.

---

## Current likely state to verify

The current implementation likely includes some combination of:

- basic orchestrator loop
- direct local model integration
- UI → orchestrator → model path
- partial tool execution wiring
- model-specific naming or dependencies in places that should stay generic

Android Studio / coding agents should verify the actual code and explicitly check for:

- direct `Gemma` calls outside provider boundaries
- orchestrator code that assumes exactly one model
- UI code that knows which model is used
- direct execution paths that bypass routing logic
- provider-specific request/response types leaking into core modules

---

## Required architecture shift

We are transitioning from:

> direct single-model execution

into:

> routed multi-model execution

This is the foundation for all future Synapse intelligence behavior.

Without this layer:
- smaller local models cannot be used correctly
- larger reasoning models will be overused
- thermal/runtime governance becomes harder
- future role separation (planner / executor / validator) becomes messy

---

## Core enforcement rule

**No component may call a model directly except through the provider/routing path.**

All model execution must go through:

```text
Orchestrator -> ModelRouter -> ModelProvider -> ModelInstance
```

This rule is non-negotiable.

If any component bypasses it, the architecture is drifting.

---

## What we are adding

We are adding a formal **model routing layer**.

This layer is responsible for:
- choosing the right model tier for a request
- escalating from cheap/small model paths to larger reasoning paths when needed
- respecting runtime/device constraints
- giving Synapse a clean path to future agent role separation

This is not an optimization.
This is part of the system identity.

---

## Required new core component: ModelRouter

### Responsibility
`ModelRouter` decides which model tier should handle a request.

It should be the single place where routing policy lives.

### Intended interface shape

```kotlin
interface ModelRouter {
    fun route(request: SynapseRequest): ModelType
}
```

This does not lock final implementation details.
It locks the architectural role.

---

## Required model-tier concept

Synapse should stop assuming "the model" is one thing.

At minimum, the runtime should be able to distinguish between:

```kotlin
enum class ModelType {
    SMALL_FAST,
    LARGE_REASONING
}
```

These labels are architectural placeholders.
The exact provider/model mapping may evolve.

---

## What these tiers mean

### SMALL_FAST
This tier should eventually handle work like:
- intent classification
- tool formatting
- schema-safe structured mapping
- light event-driven interpretation
- cheap validation/routing-style tasks

### LARGE_REASONING
This tier should eventually handle work like:
- planning
- multi-step reasoning
- synthesis
- difficult or ambiguous requests
- game-role reasoning (companion / boss / director style behavior)

This split is essential for mobile efficiency and future edge AI scaling.

---

## Required changes to ModelProvider

`ModelProvider` must stop behaving like a single concrete model hook.

It must evolve into a provider abstraction that can:
- expose multiple available model tiers
- return the appropriate model/runtime implementation for a given routed decision
- keep provider-specific implementation details out of core orchestration

### Required architectural outcome
The rest of the system should be able to ask for:

```kotlin
getModel(ModelType.SMALL_FAST)
getModel(ModelType.LARGE_REASONING)
```

without caring whether the underlying model is:
- Gemma
- Qwen
- another local runtime
- a future remote option allowed by policy

---

## Required changes to Orchestrator

The orchestrator must remain the reasoning/control loop.
But it must not become the place where model identity is hardcoded.

### Before
Possible bad pattern:
- orchestrator directly calls one concrete model implementation

### After
Required pattern:
- orchestrator produces/receives a `SynapseRequest`
- orchestrator asks `ModelRouter` for a model tier
- orchestrator gets the correct model instance via `ModelProvider`
- orchestrator continues execution without knowing concrete provider details

### Rule
The orchestrator may know:
- request shape
- model tier selection outcome
- result shape

It may not know:
- provider-specific wiring details unless explicitly abstracted
- hardcoded one-model-only assumptions

---

## Initial routing policy

The first implementation should keep routing logic simple and explicit.

### Starting policy

```text
If request is:
- classification
- formatting
- simple command interpretation
- straightforward tool mapping
-> SMALL_FAST

If request is:
- planning
- synthesis
- multi-step reasoning
- ambiguous intent
- unknown complexity
-> LARGE_REASONING
```

This policy can evolve later.
The important part is that routing exists and is centralized.

---

## Escalation policy

The first routing system should also support simple escalation.

### Required direction

```text
If SMALL_FAST fails, is uncertain, or returns low-confidence structure
-> escalate to LARGE_REASONING

If execution path breaks and retry is allowed
-> retry through LARGE_REASONING where appropriate
```

This allows Synapse to stay fast for easy work without trapping itself when a small model is not enough.

---

## Android/runtime-aware routing constraints

Because Synapse runs on Android and is intended for strong but limited local hardware, routing must remain aware of runtime conditions.

The routing layer should be designed to respect inputs such as:
- thermal state
- battery state
- charging state
- foreground/background status
- offline/online allowance
- provider health

### Initial intent

```text
If device is hot or battery is low
-> reduce or restrict LARGE_REASONING use

If offline-only mode is active
-> only local allowed models may be selected
```

This should align with `RUNTIME_GOVERNOR_V0.md`, not fight it.

---

## Future multi-agent direction

This routing layer is the foundation for future role separation.

Synapse is expected to grow toward role-based intelligence such as:
- planner
- executor
- validator
- domain/game-specific roles later

That does not mean we build a huge multi-agent system now.
It means we build the routing layer so that future separation is possible **without tearing the runtime apart later**.

Modern systems increasingly rely on structured orchestration and task-specific routing instead of one-model-does-everything design, especially when balancing speed, cost, and capability across tiers. That principle is directly relevant to Synapse’s local-first design. ([carmatec.com](https://www.carmatec.com/ai-services/ai-model-orchestration/?utm_source=chatgpt.com), [oboe.com](https://oboe.com/learn/llm-agent-orchestration-and-architecture-13v5f9b/multi-model-routing-llm-agent-orchestration-and-architecture-4?utm_source=chatgpt.com))

---

## Required inspection and refactor tasks

Android Studio / coding agents should do the following in order:

### Step 1 — inspect current implementation
Find:
- direct model calls
- direct provider-specific calls
- single-model assumptions
- UI-level model knowledge

### Step 2 — isolate the model boundary
Ensure all model access flows through provider abstraction.

### Step 3 — introduce ModelRouter
Create the routing layer and centralize model-tier decision logic.

### Step 4 — refactor orchestrator
Make orchestrator model-agnostic except for routed tier selection.

### Step 5 — preserve existing progress where possible
Do not throw away current scaffold unless it actively blocks the architecture.

### Step 6 — continue implementation only after routing exists
New model-connected features should be built on the routed path, not the old direct path.

---

## Validation checklist

This refactor is complete enough when all of the following are true:

- no direct model calls exist outside the intended provider path
- a `ModelRouter` exists
- at least two model tiers are representable in the architecture
- orchestrator is no longer hardwired to one model implementation
- UI does not decide which model is used
- the system still compiles and passes the smoke scaffold stage

---

## Failure conditions

The implementation is still wrong if any of the following remain true:

- UI directly knows or selects a concrete model implementation
- orchestrator directly instantiates or calls one specific model
- provider-specific result/request types leak across core boundaries unnecessarily
- routing logic is scattered instead of centralized
- the architecture still behaves like there is only one real model

If any of those are true, keep refactoring.

---

## Relationship to existing docs

This document should be read together with:
- `MODEL_PROVIDER_V0.md`
- `MODEL_REGISTRY_V0.md`
- `ORCHESTRATION_LOOP.md`
- `RUNTIME_GOVERNOR_V0.md`
- `SYNAPSE_ARCHITECTURE_SPEC.md`

This document does not replace those.
It tells the implementation how to **restructure current work so those documents can all remain true at once**.

---

## Final note to implementation agents

Do not interpret this as permission to endlessly redesign.

The goal is:
- keep the current progress
- reshape the architecture early
- continue building on a correct routed foundation

This is not a future optimization.
This is the layer that makes Synapse an actual runtime instead of a one-model app.
