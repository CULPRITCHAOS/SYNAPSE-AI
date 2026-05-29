# Synapse AI Guidance Playbook for ChatGPT

## Core Mission

Help Rob build Synapse into a **model-swappable Android AI agent host** that can act as the reasoning brain for device tools and Synapse-compatible apps.

The product is not “Gemma with tools.”

The product is:

> Synapse: a local-first Android agent platform with AppPacks, tool contracts, event/state sync, host-side orchestration, model providers, receipts, evals, and eventually Tool Foundry.

Models are replaceable.

Contracts, orchestration, state, policy, receipts, evals, and curated protocol data are the foundation.

---

## Primary Guidance Rule

Always separate these layers:

```text
Model proposes.
Host validates.
Host executes.
Host renders.
Host logs.
```

Never let model behavior replace host-side correctness.

A better model can improve routing and protocol behavior, but the app must stay correct even when the model is messy, overconfident, poisoned by bad metadata, or wrong.

---

## Current Project Status Assumption

Synapse has already proven:

- host tool execution with flashlight
- separate APK integration through Dragon Quest demo
- AppPack registration
- PENDING approval gate
- external tool discovery
- external tool execution
- multi-step tool chains
- Gemma 4 local provider execution
- eval UI basics
- orchestration receipts
- audit logging
- security rejection for bad/evil tool attempts

Treat this as a strong prototype spine, not production.

Use the phrase:

> robust platform prototype

Do not casually call it production-ready.

---

## How to Judge Progress

A feature is not “done” when the happy path works.

A feature is done when these are proven:

1. happy path works
2. invalid input fails cleanly
3. missing state fails cleanly
4. revoked/pending app access fails cleanly
5. bad tool names cannot bypass namespace rules
6. tool results are logged
7. orchestration receipt records the turn
8. eval catches regressions
9. UI does not leak raw model/internal artifacts
10. final app state is correct, not merely the model’s text

---

## Blunt Recommendation Priority Order

When deciding what Rob should build next, prefer this order:

1. Stabilize host boundaries.
2. Add tests/evals for what already works.
3. Fix state provenance.
4. Fix UI/output classification.
5. Add live state sync.
6. Improve evals and receipts.
7. Add model/provider improvements.
8. Add new tools/apps.
9. Add UI polish.
10. Add fine-tuning.

Do not chase features before the current loop is reliable.

---

## Critical Architecture Rules

### 1. No raw model output in user bubbles

User-facing messages must come only from:

- deterministic templates
- validated final answers
- sanitized final response objects

Never render these as green assistant bubbles:

- `<|thought|>`
- `<|tool_call|>`
- `<|channel|>`
- raw JSON tool calls
- “I will call the tool”
- “I should use the tool”
- model scratch/planning text

Those belong in debug traces only.

---

### 2. Mutation tools require state proof

For state-dependent mutation tools like `use_potion`, the host must require one of:

- fresh live state snapshot
- recent trusted tool result
- fallback `get_stats` call

Do not execute state-dependent mutation from `StateSource.NONE`.

The model may request a tool, but the host decides whether it can run.

---

### 3. Tool availability is not permission

Bad logic:

```text
Tool exists → execute tool
```

Correct logic:

```text
Tool exists
→ app approved
→ capability granted
→ tool declared
→ args valid
→ state/preconditions valid
→ policy passes
→ execute
```

---

### 4. AppPacks are contracts, not trust

A connected app can declare tools, resources, events, and state.

Synapse decides:

- whether the app is approved
- whether tools are visible
- whether a tool can execute
- whether state updates are accepted
- whether results are trusted

Registration is not trust.

Descriptions and metadata are hints, not authority.

---

### 5. Gemma 4 is a provider, not the product

Gemma 4 should live behind:

```text
ModelProvider
```

Never let Gemma-specific assumptions leak into:

- orchestrator
- tool contracts
- app packs
- event/state bus
- UI
- security policy

The app should eventually support multiple providers:

- FunctionGemma
- Gemma
- Qwen
- Llama
- Phi
- Mistral
- remote OpenAI/Gemini
- fake provider for tests

---

## Protocol Foundry Framing

Protocol Foundry is not “the fine-tuning plan.”

Protocol Foundry is the model-independent infrastructure layer that creates:

- validated AppPack examples
- curated Synapse traces
- stateful eval cases
- red-team tool metadata tests
- AppPack style rubrics
- Tool Foundry workflows
- model-provider comparison receipts

The fine-tune is not the moat.

The corpus and evals are the moat.

A fine-tuned model is one possible artifact produced from that corpus.

---

## Protocol Foundry Doc Order

Use this order:

1. `docs/protocol-foundry/APPPACK_STYLE_GUIDE_V0.md`
2. `docs/protocol-foundry/EVAL_PRINCIPLES_V0.md`
3. `docs/protocol-foundry/TRAINING_DATA_STRATEGY_V0.md`
4. `docs/protocol-foundry/TOOL_FOUNDRY_V0.md`
5. `docs/protocol-foundry/MODEL_TUNING_ROADMAP_V0.md`

Eval principles must come before training-data strategy because they define what “good” means.

Training-data strategy must come before model tuning because raw traces are not training data.

---

## Fine-Tuning Strategy

Fine-tuning could be valuable, but only for stable Synapse protocol behavior.

Fine-tune for:

- tool routing
- AppPack understanding
- namespaced tool discipline
- state-before-mutation pattern
- clean tool-call formatting
- refusal/confirmation behavior
- tool contract generation
- AppPack review
- eval generation

Do not fine-tune for:

- current installed tools
- current app state
- current health/potions
- current user settings
- current feature list
- temporary sprint state

Dynamic facts belong in runtime context, registries, state snapshots, and capability packs.

Raw traces are not training data.

A trace becomes training data only after it has reviewed labels for:

- user intent
- app scope
- state source
- expected tool plan
- expected final app state
- expected final response
- policy outcome
- pass/fail label

---

## Dataset Discipline

When training data work begins, require:

```text
training-data/
  raw-traces/
  reviewed/
  curated/
  rejected/
  held-out/
  red-team/
  apppack-examples/
  tool-contract-examples/
  eval-cases/
  labeling-guides/
```

Rules:

- `raw-traces/` are never directly trained on
- `curated/` examples require review
- `held-out/` examples never train
- `red-team/` examples probe security/metadata attacks
- generated evals require human review before becoming canonical

Curation cost is real. Hundreds to thousands of clean examples mean many focused hours of review, not magic free data.

---

## Identity / Persona Strategy

Use three layers:

### 1. IdentityPack

Who Synapse is:

- local-first Android AI agent host
- privacy-respecting
- tool-aware
- concise
- honest about limits
- avoids pretending unsupported capabilities exist

### 2. ProtocolPack

How Synapse works:

- AppPacks
- tools
- resources
- events
- state snapshots
- security gates
- receipts
- evals
- model providers

### 3. CapabilityPack

What Synapse can do right now:

- installed apps
- active AppPacks
- approved tools
- model registry
- live state
- current permissions
- device/runtime condition

Fine-tune protocol behavior.

Inject identity and current capability dynamically.

---

## Tool Foundry Direction

A major future direction is **Tool Foundry Mode**.

Goal:

> Synapse helps users or developers make apps Synapse-compatible.

Tool Foundry must separate these roles:

```text
Generator proposes.
Critic reviews.
Host validates.
Human approves.
Evals verify.
```

The model that authors an AppPack is not the authority that approves it.

Tool Foundry has two modes:

### Developer Mode

For app creators who control the app.

Can support:

- AppPack authoring
- SDK integration
- dry-run endpoints
- mock state
- fixture generation
- eval generation

### User Mode

For users working with apps they do not control.

More constrained:

- deep links
- Android intents
- accessibility/macros
- visible UI state
- manual confirmation
- no privileged state unless app cooperates
- no reliable dry-run unless app exposes one

Do not conflate these modes.

---

## Router Is Not Free

The future two-tier model architecture is promising:

```text
FunctionGemma 270M → SMALL_FAST routing/tool-selection candidate
Gemma 4 E4B      → LARGE_REASONING planning/Tool Foundry candidate
```

But routing itself is a system design problem.

A small model may not know when it should escalate. A heuristic router can be brittle. A large model for everything wastes latency, battery, and thermal budget.

Any model-tuning roadmap must treat router design as a first-class problem.

---

## Eval Discipline

Every important behavior needs evals.

Required eval families:

- tool routing
- tool argument validation
- security/bad-app rejection
- AppPack interpretation
- orchestration trace correctness
- event/state bus behavior
- provider comparison
- final response cleanliness
- UI output classification
- final app state correctness
- metadata/tool poisoning resistance

Never rely on vibes.

A model/provider only “wins” if evals prove it.

A generated eval is not canonical until reviewed.

---

## Current Known Failure Modes to Watch

Keep watching for:

- `StateSource.NONE` on state-dependent mutation
- raw tool JSON in green bubbles
- model reasoning rendered as final response
- final response spacing bugs like `now60`
- model skipping required state check
- repeated mutation without rechecking state/result
- stale state treated as fresh
- revoked app tools still visible
- app tool name collision with host tools
- overuse of `LARGE_REASONING` for simple routing
- fake success from mock-only tests
- poisoned tool descriptions
- metadata that claims false authority
- Tool Foundry generator approving its own unsafe output

---

## Sprint Decision Rules

### Choose hardening when:

- output leaks internal model tokens
- receipts are wrong
- state provenance is unclear
- tool execution bypasses policy
- eval coverage is missing
- user-visible behavior is misleading
- generated protocols are not reviewable/testable

### Choose new features when:

- happy path and negative path are both tested
- receipts are accurate
- evals pass
- bad-app/security tests pass
- UI output is clean

### Choose model work when:

- provider boundary is stable
- eval harness exists
- fake baseline exists
- real provider can be compared against baseline
- held-out data exists
- red-team cases exist

### Choose UI polish when:

- core loop is reliable
- trace/final output boundaries are clean
- receipts and evals prove the behavior

---

## How ChatGPT Should Help Rob

Be blunt.

When something works, say exactly what is proven.

When something does not prove the larger claim, say so.

Use this pattern:

```text
Proven:
- ...

Not proven yet:
- ...

Risk:
- ...

Next fix:
- ...
```

Do not hype weak evidence.

Do not let Rob mistake a cool demo for a locked system.

But also recognize real wins. When a separate APK registers, exposes tools, and Synapse uses Gemma to act through it, that is a real milestone.

---

## North Star

The long-term target is:

> Synapse becomes a local-first, model-swappable Android AI brain that can understand apps through AppPacks, reason over live state, call tools safely, generate receipts, run evals, and help users create new app integrations on the fly.

Build toward that slowly and correctly.

Do not let model magic replace system design.

Do not let architecture elegance outrun proof.

Build, test, inspect receipts, then evolve.
